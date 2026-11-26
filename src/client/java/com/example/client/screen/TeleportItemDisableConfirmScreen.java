package com.example.client.screen;

import com.example.client.i18n.ClientI18n;

import com.example.client.mvvm.ItemDisableConfirmViewModel;
import com.example.client.model.ItemManageEntry;
import com.example.network.TeleportMenuPayloads;
import io.wispforest.owo.ui.component.Components;
import io.wispforest.owo.ui.container.FlowLayout;
import com.example.client.widget.TeleportIcons;
import net.minecraft.network.chat.Component;

import java.util.Set;

public class TeleportItemDisableConfirmScreen extends BaseTeleportScreen {
	private final TeleportItemManageScreen parent;
	private final ItemDisableConfirmViewModel viewModel;

	public TeleportItemDisableConfirmScreen(ItemManageEntry entry, TeleportItemManageScreen parent, Set<String> disabledItemIds) {
		super(ClientI18n.tr("screen.teleportmenu.admin.items.confirm_title"));
		this.parent = parent;
		this.viewModel = new ItemDisableConfirmViewModel(entry, disabledItemIds);
	}

	@Override
	public void removed() {
		this.viewModel.dispose();
		super.removed();
	}

	public ItemManageEntry entry() {
		return this.viewModel.entry();
	}

	public TeleportItemManageScreen parentScreen() {
		return this.parent;
	}

	@Override
	protected void build(FlowLayout rootComponent) {
		this.configureRoot(rootComponent);
		rootComponent.gap(6);

		rootComponent.child(Components.label(ClientI18n.tr("screen.teleportmenu.admin.items.confirm_title")).shadow(true));
		rootComponent.child(Components.label(Component.literal(this.viewModel.entry().registryId())));
		rootComponent.child(Components.label(ClientI18n.tr("screen.teleportmenu.admin.items.material", this.viewModel.entry().materialLabel())));

		boolean disableSupported = this.viewModel.disableSupported();
		boolean alreadyDisabled = this.viewModel.alreadyDisabled();
		if (!disableSupported) {
			rootComponent.child(Components.label(ClientI18n.tr("screen.teleportmenu.admin.items.cannot_disable")));
		} else if (alreadyDisabled) {
			rootComponent.child(Components.label(ClientI18n.tr("screen.teleportmenu.admin.items.already_disabled")));
		}

		rootComponent.child(menuButton(
			alreadyDisabled
				? ClientI18n.tr("screen.teleportmenu.admin.items.enable")
				: ClientI18n.tr("screen.teleportmenu.admin.items.disable"),
			TeleportIcons.CONFIRM,
			() -> {
				Set<String> nextState = this.viewModel.toggle();
				if (nextState == null) {
					if (this.minecraft != null) {
						this.minecraft.setScreen(this.parent);
					}
					return;
				}

				if (this.minecraft != null) {
					this.minecraft.setScreen(new TeleportItemManageScreen(this.parent.parentScreen(), nextState, this.parent.searchQuery()));
				}
			}));
		rootComponent.child(menuButton(ClientI18n.tr("screen.teleportmenu.admin.items.cancel"), TeleportIcons.CANCEL, () -> {
			if (this.minecraft != null) {
				this.minecraft.setScreen(this.parent);
			}
		}));
	}
}
