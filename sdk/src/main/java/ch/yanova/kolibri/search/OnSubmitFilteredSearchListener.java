package ch.yanova.kolibri.search;

public interface OnSubmitFilteredSearchListener {

        void onQueryByTags(String query);

        void onQueryByText(String text);
    }