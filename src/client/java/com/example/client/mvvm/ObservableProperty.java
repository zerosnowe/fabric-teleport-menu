package com.example.client.mvvm;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;
import java.util.function.UnaryOperator;

/**
 * Minimal observable value used by the MVVM view models.
 */
public final class ObservableProperty<T> {
	private T value;
	private final List<Consumer<T>> listeners = new CopyOnWriteArrayList<>();

	public ObservableProperty(T initialValue) {
		this.value = initialValue;
	}

	public T get() {
		return this.value;
	}

	public void set(T nextValue) {
		if (Objects.equals(this.value, nextValue)) {
			return;
		}
		this.value = nextValue;
		notifyListeners();
	}

	public void update(UnaryOperator<T> operator) {
		set(operator.apply(this.value));
	}

	/**
	 * Registers a listener and immediately emits the current value.
	 */
	public Subscription observe(Consumer<T> listener) {
		this.listeners.add(listener);
		listener.accept(this.value);
		return () -> this.listeners.remove(listener);
	}

	private void notifyListeners() {
		for (Consumer<T> listener : this.listeners) {
			listener.accept(this.value);
		}
	}

	@FunctionalInterface
	public interface Subscription extends AutoCloseable {
		@Override
		void close();
	}
}
