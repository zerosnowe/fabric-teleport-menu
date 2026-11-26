package com.example.client.screen;

import com.example.client.TeleportMenuClient;
import com.example.client.i18n.ClientI18n;
import com.example.client.mvvm.MomentFeedViewModel;
import com.example.client.widget.OreSmallActionButtonWidget;
import com.example.client.widget.OreTabButtonWidget;
import com.example.client.widget.TeleportIcons;
import com.example.network.TeleportMenuPayloads;
import io.wispforest.owo.ui.component.Components;
import io.wispforest.owo.ui.component.LabelComponent;
import io.wispforest.owo.ui.container.Containers;
import io.wispforest.owo.ui.container.FlowLayout;
import io.wispforest.owo.ui.container.ScrollContainer;
import io.wispforest.owo.ui.core.Color;
import io.wispforest.owo.ui.core.Insets;
import io.wispforest.owo.ui.core.Sizing;
import io.wispforest.owo.ui.core.Surface;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Weibo-style moment feed: a single tall column of variable-height cards with a
 * newest / hottest tab header on top, global delete and public likes.
 */
public class TeleportMomentScreen extends BaseTeleportScreen {
	private static final int FEED_WIDTH = 268;
	private static final int CARD_INNER_WIDTH = FEED_WIDTH - 12;
	private static final int FEED_HEIGHT = 170;
	private static final int TAB_WIDTH = 64;
	private static final int TAB_HEIGHT = 20;
	private static final int ACTION_HEIGHT = 16;
	private static final int LIKE_WIDTH = 64;
	private static final int DELETE_WIDTH = 40;
	private static final int CARD_BACKGROUND = 0x40202836;

	private final TeleportMainScreen mainScreen;
	private final Map<UUID, OreSmallActionButtonWidget> likeButtons = new LinkedHashMap<>();
	private final List<UUID> renderedOrder = new ArrayList<>();
	private final MomentFeedViewModel viewModel;

	private FlowLayout tabRow;
	private FlowLayout feedLayout;
	private ScrollContainer<FlowLayout> feedScroll;

	public TeleportMomentScreen(TeleportMainScreen mainScreen) {
		super(ClientI18n.tr("screen.teleportmenu.moments.title"));
		this.mainScreen = mainScreen;
		this.viewModel = new MomentFeedViewModel(TeleportMenuClient.momentEntries());
	}

	public TeleportMainScreen mainScreen() {
		return this.mainScreen;
	}

	@Override
	protected void build(FlowLayout rootComponent) {
		this.configureRoot(rootComponent);
		rootComponent.gap(6);
		this.viewModel.applyFeed(List.copyOf(TeleportMenuClient.momentEntries()));
		rootComponent.child(Components.label(ClientI18n.tr("screen.teleportmenu.moments.title")).shadow(true));

		this.tabRow = Containers.horizontalFlow(Sizing.fixed(FEED_WIDTH), Sizing.fixed(TAB_HEIGHT));
		this.tabRow.gap(4);
		this.rebuildTabs();
		rootComponent.child(this.tabRow);

		this.feedLayout = Containers.verticalFlow(Sizing.fixed(FEED_WIDTH), Sizing.content());
		this.feedLayout.gap(4);

		this.feedScroll = Containers.verticalScroll(Sizing.fixed(FEED_WIDTH + 4), Sizing.fixed(FEED_HEIGHT), this.feedLayout);
		this.feedScroll.scrollbar(ScrollContainer.Scrollbar.flat(Color.ofArgb(0x90C7D5ED)));
		this.feedScroll.scrollbarThiccness(4);
		this.feedScroll.surface(Surface.BLANK);
		rootComponent.child(this.feedScroll);

		this.refreshFeed();

		rootComponent.child(this.menuButton(ClientI18n.tr("screen.teleportmenu.moments.publish"), TeleportIcons.ADD, () -> {
			if (this.minecraft != null) {
				this.minecraft.setScreen(new TeleportMomentComposeScreen(this));
			}
		}));
		rootComponent.child(this.menuButton(ClientI18n.tr("screen.teleportmenu.players.back"), TeleportIcons.BACK, () -> {
			if (this.minecraft != null) {
				this.minecraft.setScreen(this.mainScreen);
			}
		}));
		rootComponent.child(this.menuButton(ClientI18n.tr("screen.teleportmenu.players.close"), TeleportIcons.CLOSE, this::onClose));
	}

	/**
	 * Applies a server-side feed snapshot without dropping the reader's scroll
	 * position when only like counters changed.
	 */
	public void applyFeed(List<TeleportMenuPayloads.MomentEntry> entries) {
		if (entries == null) {
			return;
		}

		this.viewModel.applyFeed(entries);
		if (this.feedLayout == null) {
			return;
		}

		if (this.hasSamePosts()) {
			this.refreshLikeButtons();
			return;
		}

		this.refreshFeed();
	}

	private void switchTab(MomentFeedViewModel.Tab nextTab) {
		if (!this.viewModel.switchTab(nextTab).canExecute()) {
			return;
		}

		this.viewModel.switchTab(nextTab).execute();
		this.rebuildTabs();
		this.refreshFeed();
		if (this.feedScroll != null) {
			this.feedScroll.scrollTo(0D);
		}
	}

	private void rebuildTabs() {
		if (this.tabRow == null) {
			return;
		}

		this.tabRow.clearChildren();
		this.tabRow.child(this.tabButton(MomentFeedViewModel.Tab.LATEST, "screen.teleportmenu.moments.tab.latest"));
		this.tabRow.child(this.tabButton(MomentFeedViewModel.Tab.HOT, "screen.teleportmenu.moments.tab.hot"));
	}

	private io.wispforest.owo.ui.core.Component tabButton(MomentFeedViewModel.Tab tabTarget, String labelKey) {
		OreTabButtonWidget widget = new OreTabButtonWidget(
			TAB_WIDTH,
			TAB_HEIGHT,
			ClientI18n.tr(labelKey),
			this.viewModel.tab().get() == tabTarget,
			button -> this.switchTab(tabTarget)
		);
		var wrapped = Components.wrapVanillaWidget(widget);
		wrapped.sizing(Sizing.fixed(TAB_WIDTH), Sizing.fixed(TAB_HEIGHT));
		return wrapped;
	}

	private void refreshFeed() {
		if (this.feedLayout == null) {
			return;
		}

		this.feedLayout.clearChildren();
		this.likeButtons.clear();
		this.renderedOrder.clear();

		List<TeleportMenuPayloads.MomentEntry> sorted = this.sortedMoments();
		if (sorted.isEmpty()) {
			this.feedLayout.child(Components.label(ClientI18n.tr("screen.teleportmenu.moments.empty")));
			return;
		}

		for (TeleportMenuPayloads.MomentEntry moment : sorted) {
			FlowLayout card = this.buildCard(moment);
			this.renderedOrder.add(moment.id());
			this.feedLayout.child(card);
		}
	}

	private List<TeleportMenuPayloads.MomentEntry> sortedMoments() {
		return this.viewModel.sortedMoments();
	}

	private FlowLayout buildCard(TeleportMenuPayloads.MomentEntry moment) {
		FlowLayout card = Containers.verticalFlow(Sizing.fixed(FEED_WIDTH), Sizing.content());
		card.padding(Insets.of(6));
		card.gap(3);
		card.surface(Surface.flat(CARD_BACKGROUND));

		String authorName = moment.own()
			? moment.authorName() + ClientI18n.text("screen.teleportmenu.moments.own_suffix")
			: moment.authorName();
		card.child(Components.label(Component.literal(authorName + "  ·  " + relativeTime(moment.createdAt())))
			.shadow(true)
			.color(Color.ofArgb(0xFFB8C6DA)));

		LabelComponent content = Components.label(Component.literal(moment.content()));
		content.maxWidth(CARD_INNER_WIDTH);
		content.color(Color.ofArgb(0xFFF2F6FF));
		card.child(content);

		FlowLayout actions = Containers.horizontalFlow(Sizing.fixed(CARD_INNER_WIDTH), Sizing.fixed(ACTION_HEIGHT));
		actions.gap(4);

		OreSmallActionButtonWidget likeButton = new OreSmallActionButtonWidget(
			LIKE_WIDTH,
			ACTION_HEIGHT,
			likeLabel(moment),
			button -> this.send(new TeleportMenuPayloads.LikeMomentPayload(moment.id()))
		);
		var likeComponent = Components.wrapVanillaWidget(likeButton);
		likeComponent.sizing(Sizing.fixed(LIKE_WIDTH), Sizing.fixed(ACTION_HEIGHT));
		actions.child(likeComponent);
		this.likeButtons.put(moment.id(), likeButton);

		OreSmallActionButtonWidget deleteButton = new OreSmallActionButtonWidget(
			DELETE_WIDTH,
			ACTION_HEIGHT,
			ClientI18n.tr("screen.teleportmenu.moments.delete"),
			button -> this.send(new TeleportMenuPayloads.DeleteMomentPayload(moment.id()))
		);
		var deleteComponent = Components.wrapVanillaWidget(deleteButton);
		deleteComponent.sizing(Sizing.fixed(DELETE_WIDTH), Sizing.fixed(ACTION_HEIGHT));
		actions.child(deleteComponent);

		card.child(actions);
		return card;
	}

	private void refreshLikeButtons() {
		for (TeleportMenuPayloads.MomentEntry moment : this.viewModel.moments().get()) {
			OreSmallActionButtonWidget button = this.likeButtons.get(moment.id());
			if (button != null) {
				button.setMessage(likeLabel(moment));
			}
		}
	}

	private boolean hasSamePosts() {
		List<TeleportMenuPayloads.MomentEntry> moments = this.viewModel.moments().get();
		if (moments.size() != this.renderedOrder.size()) {
			return false;
		}

		Set<UUID> incoming = new HashSet<>();
		for (TeleportMenuPayloads.MomentEntry moment : moments) {
			incoming.add(moment.id());
		}

		for (UUID renderedId : this.renderedOrder) {
			if (!incoming.contains(renderedId)) {
				return false;
			}
		}

		return true;
	}

	private static net.minecraft.network.chat.Component likeLabel(TeleportMenuPayloads.MomentEntry moment) {
		if (moment.likedByViewer()) {
			return ClientI18n.tr("screen.teleportmenu.moments.liked", moment.likeCount());
		}

		return ClientI18n.tr("screen.teleportmenu.moments.like", moment.likeCount());
	}

	private void send(CustomPacketPayload payload) {
		if (TeleportMenuClient.sendMomentPayload(payload)) {
			return;
		}

		if (this.minecraft != null && this.minecraft.player != null) {
			this.minecraft.player.sendSystemMessage(ClientI18n.tr("message.teleportmenu.moment_unsupported"));
		}
	}

	private static String relativeTime(long createdAt) {
		long elapsed = Math.max(0L, System.currentTimeMillis() - createdAt);
		if (elapsed < 60_000L) {
			return ClientI18n.text("screen.teleportmenu.moments.time.just_now");
		}

		long minutes = elapsed / 60_000L;
		if (minutes < 60L) {
			return ClientI18n.text("screen.teleportmenu.moments.time.minutes", minutes);
		}

		long hours = minutes / 60L;
		if (hours < 24L) {
			return ClientI18n.text("screen.teleportmenu.moments.time.hours", hours);
		}

		return ClientI18n.text("screen.teleportmenu.moments.time.days", hours / 24L);
	}
}
