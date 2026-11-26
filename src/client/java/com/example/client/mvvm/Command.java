package com.example.client.mvvm;

import java.util.function.BooleanSupplier;

/**
 * Executable action exposed by a view model to its screen.
 */
public final class Command {
	private final Runnable action;
	private final BooleanSupplier canExecute;

	public Command(Runnable action) {
		this(action, () -> true);
	}

	public Command(Runnable action, BooleanSupplier canExecute) {
		this.action = action;
		this.canExecute = canExecute;
	}

	public boolean canExecute() {
		return this.canExecute.getAsBoolean();
	}

	public void execute() {
		if (canExecute()) {
			this.action.run();
		}
	}
}
