/*
 * Copyright (C) 2011 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.internal.view.menu;

import android.content.Context;
import android.os.Bundle;
import android.os.Parcelable;
import android.util.SparseArray;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ListAdapter;

import java.util.ArrayList;

/**
 * MenuPresenter for list-style menus.
 */
public class ListMenuPresenter implements MenuPresenter, AdapterView.OnItemClickListener {
    Context mContext;
    LayoutInflater mInflater;
    MenuBuilder mMenu;

    ExpandedMenuView mMenuView;

    private int mItemIndexOffset;
    int mThemeRes;
    int mItemLayoutRes;

    private Callback mCallback;
    private MenuAdapter mAdapter;

    public static final String VIEWS_TAG = "android:menu:list";

    /**
     * Construct a new ListMenuPresenter.
     * @param context Context to use for theming. This will supersede the context provided
     *                to initForMenu when this presenter is added.
     * @param itemLayoutRes Layout resource for individual item views.
     */
    public ListMenuPresenter(Context context, int itemLayoutRes) {
        this(itemLayoutRes, 0);
        mContext = context;
    }

    /**
     * Construct a new ListMenuPresenter.
     * @param itemLayoutRes Layout resource for individual item views.
     * @param themeRes Resource ID of a theme to use for views.
     */
    public ListMenuPresenter(int itemLayoutRes, int themeRes) {
        mItemLayoutRes = itemLayoutRes;
        mThemeRes = themeRes;
    }

    @Override
    public void initForMenu(Context context, MenuBuilder menu) {
        if (mThemeRes != 0) {
            mContext = new ContextThemeWrapper(context, mThemeRes);
        } else if (mContext == null) {
            mContext = context;
        }
        mInflater = LayoutInflater.from(mContext);
        mMenu = menu;
    }

    @Override
    public MenuView getMenuView(ViewGroup root) {
        if (mMenuView == null) {
            mMenuView = (ExpandedMenuView) mInflater.inflate(
                    com.android.internal.R.layout.expanded_menu_layout, root, false);
            if (mAdapter == null) {
                mAdapter = new MenuAdapter();
            }
            mMenuView.setAdapter(mAdapter);
            mMenuView.setOnItemClickListener(this);
        }
        return mMenuView;
    }

    /**
     * Call this instead of getMenuView if you want to manage your own ListView.
     * For proper operation, the ListView hosting this adapter should add
     * this presenter as an OnItemClickListener.
     *
     * @return A ListAdapter containing the items in the menu.
     */
    public ListAdapter getAdapter() {
        if (mAdapter == null) {
            mAdapter = new MenuAdapter();
        }
        return mAdapter;
    }

    @Override
    public void updateMenuView(boolean cleared) {
        if (mAdapter != null) mAdapter.notifyDataSetChanged();
    }

    @Override
    public void setCallback(Callback cb) {
        mCallback = cb;
    }

    @Override
    public boolean onSubMenuSelected(SubMenuBuilder subMenu) {
        if (!subMenu.hasVisibleItems()) return false;

        // The window manager will give us a token.
        new MenuDialogHelper(subMenu).show(null);
        if (mCallback != null) {
            mCallback.onOpenSubMenu(subMenu);
        }
        return true;
    }

    @Override
    public void onCloseMenu(MenuBuilder menu, boolean allMenusAreClosing) {
        if (mCallback != null) {
            mCallback.onCloseMenu(menu, allMenusAreClosing);
        }
    }

    int getItemIndexOffset() {
        return mItemIndexOffset;
    }

    public void setItemIndexOffset(int offset) {
        mItemIndexOffset = offset;
        if (mMenuView != null) {
            updateMenuView(false);
        }
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        mMenu.performItemAction(mAdapter.getItem(position), 0);
    }

    @Override
    public boolean flagActionItems() {
        return false;
    }

    public boolean expandItemActionView(MenuBuilder menu, MenuItemImpl item) {
        return false;
    }

    public boolean collapseItemActionView(MenuBuilder menu, MenuItemImpl item) {
        return false;
    }

    public void saveHierarchyState(Bundle outState) {
        SparseArray<Parcelable> viewStates = new SparseArray<Parcelable>();
        if (mMenuView != null) {
            ((View) mMenuView).saveHierarchyState(viewStates);
        }
        outState.putSparseParcelableArray(VIEWS_TAG, viewStates);
    }

    public void restoreHierarchyState(Bundle inState) {
        SparseArray<Parcelable> viewStates = inState.getSparseParcelableArray(VIEWS_TAG);
        if (viewStates != null) {
            ((View) mMenuView).restoreHierarchyState(viewStates);
        }
    }

    private class MenuAdapter extends BaseAdapter {
        public int getCount() {
            ArrayList<MenuItemImpl> items = mMenu.getVisibleItems();
            return items.size() - mItemIndexOffset;
        }

        public MenuItemImpl getItem(int position) {
            ArrayList<MenuItemImpl> items = mMenu.getVisibleItems();
            return items.get(position + mItemIndexOffset);
        }

        public long getItemId(int position) {
            // Since a menu item's ID is optional, we'll use the position as an
            // ID for the item in the AdapterView
            return position;
        }

        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = mInflater.inflate(mItemLayoutRes, parent, false);
            }

            MenuView.ItemView itemView = (MenuView.ItemView) convertView;
            itemView.initialize(getItem(position), 0);
            return convertView;
        }
    }
}
