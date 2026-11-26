package com.example.client.mvvm;

import com.example.network.TeleportMenuPayloads;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Drives the moment feed: cached snapshot, tab ordering and server actions.
 */
public final class MomentFeedViewModel extends ViewModel {
	public enum Tab {
		LATEST,
		HOT
	}

	private final ObservableProperty<List<TeleportMenuPayloads.MomentEntry>> moments;
	private final ObservableProperty<Tab> tab = property(Tab.LATEST);

	public MomentFeedViewModel(List<TeleportMenuPayloads.MomentEntry> initialMoments) {
		this.moments = property(List.copyOf(initialMoments == null ? List.of() : initialMoments));
	}

	public ObservableProperty<List<TeleportMenuPayloads.MomentEntry>> moments() {
		return this.moments;
	}

	public ObservableProperty<Tab> tab() {
		return this.tab;
	}

	public void applyFeed(List<TeleportMenuPayloads.MomentEntry> entries) {
		if (entries == null) {
			return;
		}
		this.moments.set(List.copyOf(entries));
	}

	public List<TeleportMenuPayloads.MomentEntry> sortedMoments() {
		List<TeleportMenuPayloads.MomentEntry> sorted = new ArrayList<>(this.moments.get());
		Comparator<TeleportMenuPayloads.MomentEntry> newestFirst =
			Comparator.comparingLong(TeleportMenuPayloads.MomentEntry::createdAt).reversed();

		if (this.tab.get() == Tab.HOT) {
			sorted.sort(Comparator
				.comparingInt(TeleportMenuPayloads.MomentEntry::likeCount)
				.reversed()
				.thenComparing(newestFirst));
		} else {
			sorted.sort(newestFirst);
		}

		return sorted;
	}

	public Command switchTab(Tab nextTab) {
		return new Command(() -> this.tab.set(nextTab), () -> this.tab.get() != nextTab);
	}
}
