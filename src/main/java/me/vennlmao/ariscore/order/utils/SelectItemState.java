package me.vennlmao.ariscore.order.utils;

public final class SelectItemState {
   private int page = 1;
   private int sortIndex = 0;
   private int filterIndex = 0;
   private String searchQuery = "";

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
