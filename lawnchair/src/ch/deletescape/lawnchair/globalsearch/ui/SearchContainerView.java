/*
 *     Copyright (C) 2020 Lawnchair Team.
 *
 *     This file is part of Lawnchair Launcher.
 *
 *     Lawnchair Launcher is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     Lawnchair Launcher is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with Lawnchair Launcher.  If not, see <https://www.gnu.org/licenses/>.
 */

package ch.deletescape.lawnchair.globalsearch.ui;

import static com.android.launcher3.LauncherState.NORMAL;

import android.content.Context;
import android.graphics.Point;
import android.graphics.Rect;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.widget.LinearLayoutManager;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;
import ch.deletescape.lawnchair.LawnchairPreferences;
import com.android.launcher3.AbstractFloatingView;
import com.android.launcher3.DeviceProfile;
import com.android.launcher3.DeviceProfile.OnDeviceProfileChangeListener;
import com.android.launcher3.Insettable;
import com.android.launcher3.InsettableFrameLayout;
import com.android.launcher3.Launcher;
import com.android.launcher3.R;
import com.android.launcher3.allapps.AllAppsGridAdapter;
import com.android.launcher3.allapps.AllAppsRecyclerView;
import com.android.launcher3.allapps.AllAppsStore;
import com.android.launcher3.allapps.AlphabeticalAppsList;
import com.android.launcher3.allapps.SearchUiManager;
import com.android.launcher3.keyboard.FocusedItemDecorator;
import com.android.launcher3.views.RecyclerViewFastScroller;
import com.android.launcher3.views.SpringRelativeLayout;
import com.google.android.apps.nexuslauncher.qsb.AllAppsQsbLayout;

public class SearchContainerView extends RelativeLayout implements Insettable,
        OnDeviceProfileChangeListener {

    private Launcher mLauncher;
    private AllAppsStore mAllAppsStore = new AllAppsStore();
    private View mSearchContainer;
    private SearchUiManager mSearchUiManager;
    private AdapterHolder mAH;
    private String mLastSearchQuery;

    public SearchContainerView(Context context) {
        this(context, null);
    }

    public SearchContainerView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public SearchContainerView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        mLauncher = Launcher.getLauncher(context);

        mAllAppsStore.addUpdateListener(this::onAppsUpdated);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();

//        // This is a focus listener that proxies focus from a view into the list view.  This is to
//        // work around the search box from getting first focus and showing the cursor.
        setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus && getActiveRecyclerView() != null) {
                getActiveRecyclerView().requestFocus();
            }
        });
//
        rebindAdapters();

        mSearchContainer = findViewById(R.id.search_container_all_apps);
        mSearchUiManager = (SearchUiManager) mSearchContainer;
        mSearchUiManager.initialize(this);
    }

    @Override
    public void onDeviceProfileChanged(DeviceProfile dp) {
        AdapterHolder holder = mAH;
        if (holder.recyclerView != null) {
            // Remove all views and clear the pool, while keeping the data same. After this
            // call, all the viewHolders will be recreated.
            holder.recyclerView.swapAdapter(holder.recyclerView.getAdapter(), true);
            holder.recyclerView.getRecycledViewPool().clear();
        }
    }

    private void onAppsUpdated() {

    }

    public void refreshAppsList(){
        mAH.appsList.onAppsUpdated();
    }

    private void rebindAdapters() {
        createHolders();
        replaceRVContainer();
        mAH.setup(findViewById(R.id.apps_list_view));
//        if (mAH.recyclerView != null) {
//            mAH.recyclerView.bindFastScrollbar();
//        }
    }

    private void createHolders() {
        mAH = createHolder();
    }

    private void replaceRVContainer() {
        if (mAH.recyclerView != null) {
            mAH.recyclerView.setLayoutManager(null);
        }
        View oldView = getRecyclerViewContainer();
        int index = indexOfChild(oldView);
        removeView(oldView);
        int layout = R.layout.all_apps_rv_layout;
        View newView = LayoutInflater.from(getContext()).inflate(layout, this, false);
        addView(newView, index);
    }

    public View getRecyclerViewContainer() {
        return findViewById(R.id.apps_list_view);
    }

    public SearchUiManager getSearchUiManager() {
        return mSearchUiManager;
    }

    public AllAppsStore getAppsStore() {
        return mAllAppsStore;
    }

    public AlphabeticalAppsList getApps() {
        return mAH.appsList;
    }

    public void reset(boolean animate) {
        reset(animate, false);
    }

    public void reset(boolean animate, boolean force) {
        if (force && mAH.recyclerView != null) {
            mAH.recyclerView.scrollToTop();
        }
        // Reset the search bar and base recycler view after transitioning home
        mSearchUiManager.resetSearch();
    }
//
//    @Override
//    protected void handleClose(boolean animate) {
//
//    }
//
//    @Override
//    public void logActionCommand(int command) {
//
//    }
//
//    @Override
//    protected boolean isOfType(int type) {
//        return (type & TYPE_SEARCH) != 0;
//    }


    @Override
    public void setInsets(Rect insets) {
        DeviceProfile grid = mLauncher.getDeviceProfile();
        int leftRightPadding = grid.desiredWorkspaceLeftRightMarginPx
                + grid.cellLayoutPaddingLeftRightPx;

        mAH.setPadding(leftRightPadding, insets.bottom);

        ViewGroup.MarginLayoutParams mlp = (MarginLayoutParams) getLayoutParams();
        if (grid.isVerticalBarLayout()) {
            mlp.leftMargin = insets.left;
            mlp.rightMargin = insets.right;
            setPadding(grid.workspacePadding.left, 0, grid.workspacePadding.right, 0);
        } else {
            if (!LawnchairPreferences.Companion.getInstance(getContext()).getAllAppsSearch()) {
                AllAppsQsbLayout qsb = (AllAppsQsbLayout) mSearchContainer;
                mlp.topMargin = -(qsb.getTopMargin(insets) + qsb.getLayoutParams().height);
            }
            mlp.leftMargin = mlp.rightMargin = 0;
            setPadding(0, 0, 0, 0);
        }
        setLayoutParams(mlp);

//        mNavBarScrimHeight = insets.bottom;
        InsettableFrameLayout.dispatchInsets(this, insets);
    }
//
//    @Override
//    public boolean onControllerInterceptTouchEvent(MotionEvent ev) {
//        return false;
//    }


    public void setLastSearchQuery(String query) {
        mLastSearchQuery = query;
        mAH.adapter.setLastSearchQuery(query);
    }

    public void onClearSearchResult() {
    }

    public void onSearchResultsChanged() {
        if (mAH.recyclerView != null) {
            mAH.recyclerView.onSearchResultsChanged();
        }
    }

    public AllAppsRecyclerView getActiveRecyclerView() {
        return mAH.recyclerView;
    }

    public AdapterHolder createHolder() {
        return new AdapterHolder();
    }

    public class AdapterHolder {
        public static final int MAIN = 0;

        public final AllAppsGridAdapter adapter;
        final LinearLayoutManager layoutManager;
        final AlphabeticalAppsList appsList;
        public final Rect padding = new Rect();
        public AllAppsRecyclerView recyclerView;
        boolean verticalFadingEdge;

        AdapterHolder() {
            appsList = new AlphabeticalAppsList(mLauncher, mAllAppsStore);
            adapter = new AllAppsGridAdapter(mLauncher, appsList);
            appsList.setAdapter(adapter);
            layoutManager = adapter.getLayoutManager();
        }

        public void setup(@NonNull View rv) {
            recyclerView = (AllAppsRecyclerView) rv;
//            recyclerView.setEdgeEffectFactory(createEdgeEffectFactory());
            recyclerView.setApps(appsList);
            recyclerView.setLayoutManager(layoutManager);
            recyclerView.setAdapter(adapter);
            recyclerView.setHasFixedSize(true);
            // No animations will occur when changes occur to the items in this RecyclerView.
            recyclerView.setItemAnimator(null);
            FocusedItemDecorator focusedItemDecorator = new FocusedItemDecorator(recyclerView);
            recyclerView.addItemDecoration(focusedItemDecorator);
            applyVerticalFadingEdgeEnabled(verticalFadingEdge);
            applyPadding();
        }

        public void setPadding(int horizontal, int bottom) {
            padding.bottom = bottom;
            padding.left = horizontal;
            padding.right = horizontal;
            applyPadding();
        }

        public void applyPadding() {
            if (recyclerView != null) {
                recyclerView.setPadding(padding.left, padding.top, padding.right, padding.bottom);
            }
        }

        public void applyVerticalFadingEdgeEnabled(boolean enabled) {
            verticalFadingEdge = enabled;
            mAH.recyclerView.setVerticalFadingEdgeEnabled(verticalFadingEdge);
        }

    }
}
