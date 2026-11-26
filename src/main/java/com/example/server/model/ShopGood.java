package com.example.server.model;

/**
 * A single item offered for sale inside a player shop.
 */
public final class ShopGood {
	private final String itemId;
	private final String name;
	private final String iconItemId;
	private int stock;
	private double price;

	public ShopGood(String itemId, String name, String iconItemId, int stock, double price) {
		this.itemId = itemId == null ? "" : itemId;
		this.name = name == null ? "" : name;
		this.iconItemId = iconItemId == null ? this.itemId : iconItemId;
		this.stock = Math.max(0, stock);
		this.price = Math.max(0D, price);
	}

	public String itemId() { return this.itemId; }
	public String name() { return this.name; }
	public String iconItemId() { return this.iconItemId; }
	public int stock() { return this.stock; }
	public double price() { return this.price; }

	public void setStock(int stock) { this.stock = Math.max(0, stock); }
	public void setPrice(double price) { this.price = Math.max(0D, price); }
}
