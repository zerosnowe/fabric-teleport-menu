package com.example.client.mvvm;

import java.util.ArrayList;
import java.util.List;

/**
 * Base class owning bindings so screens can release them when they close.
 */
public abstract class ViewModel {
	private final List<ObservableProperty.Subscription> subscriptions = new ArrayList<>();

	protected <T> ObservableProperty<T> property(T initialValue) {
		return new ObservableProperty<>(initialValue);
	}

	protected void track(ObservableProperty.Subscription subscription) {
		this.subscriptions.add(subscription);
	}

	public void dispose() {
		for (ObservableProperty.Subscription subscription : this.subscriptions) {
			subscription.close();
		}
		this.subscriptions.clear();
		onDispose();
	}

	protected void onDispose() {
	}
}
