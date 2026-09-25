package me.vennlmao.ariscore.order.utils;

import java.util.UUID;

public final class OrdersState {
   private final UUID playerId;
   private int page;
   private int sortIndex;
   private int filterIndex;
   private String searchQuery;

   public OrdersState(UUID playerId) {
      this.playerId = playerId;
      this.page = 1;
      this.sortIndex = 0;
      this.filterIndex = 0;
      this.searchQuery = "";
   }

   public UUID getPlayerId() {
      return this.playerId;
   }

   public int getPage() {
      return this.page;
   }

   public void setPage(int page) {
      this.page = page;
   }

   public int getSortIndex() {
      return this.sortIndex;
   }

   public void setSortIndex(int sortIndex) {
      this.sortIndex = sortIndex;
   }

   public int getFilterIndex() {
      return this.filterIndex;
   }

   public void setFilterIndex(int filterIndex) {
      this.filterIndex = filterIndex;
   }

   public String getSearchQuery() {
      return this.searchQuery;
   }

   public void setSearchQuery(String searchQuery) {
      this.searchQuery = searchQuery == null ? "" : searchQuery;
   }
}
