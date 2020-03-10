package com.google.android.apps.nexuslauncher.qsb;

import android.content.Context;
import android.content.Intent;
import android.util.AttributeSet;
import ch.deletescape.lawnchair.globalsearch.SearchProvider;
import ch.deletescape.lawnchair.globalsearch.SearchProviderController;
import ch.deletescape.lawnchair.globalsearch.providers.web.WebSearchProvider;
import ch.deletescape.lawnchair.globalsearch.ui.SearchContainerView;
import com.android.launcher3.ExtendedEditText;
import com.android.launcher3.Launcher;
import com.android.launcher3.allapps.AllAppsStore.OnUpdateListener;
import com.android.launcher3.allapps.AlphabeticalAppsList;
import com.android.launcher3.allapps.search.AllAppsSearchBarController;
import com.android.launcher3.allapps.search.AllAppsSearchBarController.Callbacks;
import com.android.launcher3.util.ComponentKey;

import java.util.ArrayList;
import java.util.List;

public class FallbackAppsSearchView extends ExtendedEditText implements OnUpdateListener, Callbacks {
    final AllAppsSearchBarController DI;
    AllAppsQsbLayout DJ;
    AlphabeticalAppsList mApps;
    SearchContainerView mSearchContainerView;

    public FallbackAppsSearchView(Context context) {
        this(context, null);
    }

    public FallbackAppsSearchView(Context context, AttributeSet attributeSet) {
        this(context, attributeSet, 0);
    }

    public FallbackAppsSearchView(Context context, AttributeSet attributeSet, int i) {
        super(context, attributeSet, i);
        this.DI = new AllAppsSearchBarController();
    }

    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        Launcher.getLauncher(getContext()).getSearchView().getAppsStore().addUpdateListener(this);
    }

    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        Launcher.getLauncher(getContext()).getSearchView().getAppsStore().removeUpdateListener(this);
    }

    @Override
    public void onSearchResult(String query, ArrayList<ComponentKey> apps) {
        if (getParent() != null) {
            if (apps != null) {
                mApps.setOrderedFilter(apps);
            }
            if (apps != null) {
                dV();
                x(true);
                mSearchContainerView.setLastSearchQuery(query);
            }
        }
    }

    @Override
    public void onSuggestions(List<String> suggestions) {
        if (getParent() != null) {
            if (suggestions != null) {
                mApps.setSearchSuggestions(suggestions);
            }
        }
    }

    @Override
    public final void clearSearchResult() {
        if (getParent() != null) {
            if (mApps.setOrderedFilter(null) || mApps.setSearchSuggestions(null)) {
                dV();
            }
            x(false);
            DJ.mDoNotRemoveFallback = true;
            mSearchContainerView.onClearSearchResult();
            DJ.mDoNotRemoveFallback = false;
        }
    }

    @Override
    public boolean onSubmitSearch() {
        if (mApps.hasNoFilteredResults()) {
            return false;
        }
        SearchProvider provider = getSearchProvider();
        if (provider instanceof WebSearchProvider) {
            ((WebSearchProvider) provider).openResults(getText().toString());
            return true;
        }
        Intent i = mApps.getFilteredApps().get(0).getIntent();
        getContext().startActivity(i);
        return true;
    }

    public void onAppsUpdated() {
        this.DI.refreshSearchResult();
    }

    private void x(boolean z) {
//        PredictionsFloatingHeader predictionsFloatingHeader = (PredictionsFloatingHeader) mAppsView.getFloatingHeaderView();
//        predictionsFloatingHeader.setCollapsed(z);
    }

    private void dV() {
        this.DJ.setShadowAlpha(0);
        mSearchContainerView.onSearchResultsChanged();
    }

    private SearchProvider getSearchProvider() {
        return SearchProviderController.Companion.getInstance(getContext()).getSearchProvider();
    }
}
