/*
 * Copyright (C) 2010 The Android Open Source Project
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

package android.app;

import android.graphics.drawable.Drawable;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.SpinnerAdapter;

/**
 * This is the public interface to the contextual ActionBar.
 * The ActionBar acts as a replacement for the title bar in Activities.
 * It provides facilities for creating toolbar actions as well as
 * methods of navigating around an application. 
 */
public abstract class ActionBar {
    /**
     * Standard navigation mode. Consists of either a logo or icon
     * and title text with an optional subtitle. Clicking any of these elements
     * will dispatch onActionItemSelected to the registered Callback with
     * a MenuItem with item ID android.R.id.home.
     */
    public static final int NAVIGATION_MODE_STANDARD = 0;
    
    /**
     * Dropdown list navigation mode. Instead of static title text this mode
     * presents a dropdown menu for navigation within the activity.
     */
    public static final int NAVIGATION_MODE_DROPDOWN_LIST = 1;
    
    /**
     * Tab navigation mode. Instead of static title text this mode
     * presents a series of tabs for navigation within the activity.
     */
    public static final int NAVIGATION_MODE_TABS = 2;
    
    /**
     * Custom navigation mode. This navigation mode is set implicitly whenever
     * a custom navigation view is set. See {@link #setCustomNavigationMode(View)}.
     */
    public static final int NAVIGATION_MODE_CUSTOM = 3;

    /**
     * Use logo instead of icon if available. This flag will cause appropriate
     * navigation modes to use a wider logo in place of the standard icon.
     */
    public static final int DISPLAY_USE_LOGO = 0x1;
    
    /**
     * Hide 'home' elements in this action bar, leaving more space for other
     * navigation elements. This includes logo and icon.
     */
    public static final int DISPLAY_HIDE_HOME = 0x2;

    /**
     * Set the action bar into custom navigation mode, supplying a view
     * for custom navigation.
     * 
     * Custom navigation views appear between the application icon and
     * any action buttons and may use any space available there. Common
     * use cases for custom navigation views might include an auto-suggesting
     * address bar for a browser or other navigation mechanisms that do not
     * translate well to provided navigation modes.
     * 
     * @param view Custom navigation view to place in the ActionBar.
     */
    public abstract void setCustomNavigationMode(View view);
    
    /**
     * Set the action bar into dropdown navigation mode and supply an adapter
     * that will provide views for navigation choices.
     * 
     * @param adapter An adapter that will provide views both to display
     *                the current navigation selection and populate views
     *                within the dropdown navigation menu.
     * @param callback A NavigationCallback that will receive events when the user
     *                 selects a navigation item.
     */
    public abstract void setDropdownNavigationMode(SpinnerAdapter adapter,
            NavigationCallback callback);

    /**
     * Set the action bar into standard navigation mode, supplying a title and subtitle.
     * 
     * Standard navigation mode is default. The title is automatically set to the
     * name of your Activity. Subtitles are displayed underneath the title, usually
     * in a smaller font or otherwise less prominently than the title. Subtitles are
     * good for extended descriptions of activity state.
     *
     * @param title The action bar's title. null is treated as an empty string.
     * @param subtitle The action bar's subtitle. null will remove the subtitle entirely.
     */
    public abstract void setStandardNavigationMode(CharSequence title, CharSequence subtitle);

    /**
     * Set the action bar into standard navigation mode, supplying a title and subtitle.
     * 
     * Standard navigation mode is default. The title is automatically set to the
     * name of your Activity on startup if an action bar is present.
     *
     * @param title The action bar's title. null is treated as an empty string.
     */
    public abstract void setStandardNavigationMode(CharSequence title);

    /**
     * Set the action bar into standard navigation mode, using the currently set title
     * and/or subtitle.
     *
     * Standard navigation mode is default. The title is automatically set to the name of
     * your Activity on startup if an action bar is present.
     */
    public abstract void setStandardNavigationMode();

    /**
     * Set the action bar's title. This will only be displayed in standard navigation mode.
     *
     * @param title Title to set
     */
    public abstract void setTitle(CharSequence title);

    /**
     * Set the action bar's subtitle. This will only be displayed in standard navigation mode.
     * Set to null to disable the subtitle entirely.
     *
     * @param subtitle Subtitle to set
     */
    public abstract void setSubtitle(CharSequence subtitle);

    /**
     * Set display options. This changes all display option bits at once. To change
     * a limited subset of display options, see {@link #setDisplayOptions(int, int)}.
     * 
     * @param options A combination of the bits defined by the DISPLAY_ constants
     *                defined in ActionBar.
     */
    public abstract void setDisplayOptions(int options);
    
    /**
     * Set selected display options. Only the options specified by mask will be changed.
     * To change all display option bits at once, see {@link #setDisplayOptions(int)}.
     * 
     * <p>Example: setDisplayOptions(0, DISPLAY_HIDE_HOME) will disable the
     * {@link #DISPLAY_HIDE_HOME} option.
     * setDisplayOptions(DISPLAY_HIDE_HOME, DISPLAY_HIDE_HOME | DISPLAY_USE_LOGO)
     * will enable {@link #DISPLAY_HIDE_HOME} and disable {@link #DISPLAY_USE_LOGO}.
     * 
     * @param options A combination of the bits defined by the DISPLAY_ constants
     *                defined in ActionBar.
     * @param mask A bit mask declaring which display options should be changed.
     */
    public abstract void setDisplayOptions(int options, int mask);
    
    /**
     * Set the ActionBar's background.
     * 
     * @param d Background drawable
     */
    public abstract void setBackgroundDrawable(Drawable d);
    
    /**
     * @return The current custom navigation view.
     */
    public abstract View getCustomNavigationView();
    
    /**
     * Returns the current ActionBar title in standard mode.
     * Returns null if {@link #getNavigationMode()} would not return
     * {@link #NAVIGATION_MODE_STANDARD}. 
     *
     * @return The current ActionBar title or null.
     */
    public abstract CharSequence getTitle();
    
    /**
     * Returns the current ActionBar subtitle in standard mode.
     * Returns null if {@link #getNavigationMode()} would not return
     * {@link #NAVIGATION_MODE_STANDARD}. 
     *
     * @return The current ActionBar subtitle or null.
     */
    public abstract CharSequence getSubtitle();
    
    /**
     * Returns the current navigation mode. The result will be one of:
     * <ul>
     * <li>{@link #NAVIGATION_MODE_STANDARD}</li>
     * <li>{@link #NAVIGATION_MODE_DROPDOWN_LIST}</li>
     * <li>{@link #NAVIGATION_MODE_TABS}</li>
     * <li>{@link #NAVIGATION_MODE_CUSTOM}</li>
     * </ul>
     *
     * @return The current navigation mode.
     * 
     * @see #setStandardNavigationMode()
     * @see #setStandardNavigationMode(CharSequence)
     * @see #setStandardNavigationMode(CharSequence, CharSequence)
     * @see #setDropdownNavigationMode(SpinnerAdapter)
     * @see #setTabNavigationMode()
     * @see #setCustomNavigationMode(View)
     */
    public abstract int getNavigationMode();
    
    /**
     * @return The current set of display options. 
     */
    public abstract int getDisplayOptions();

    /**
     * Start a context mode controlled by <code>callback</code>.
     * The {@link ContextModeCallback} will receive lifecycle events for the duration
     * of the context mode.
     *
     * @param callback Callback handler that will manage this context mode.
     */
    public abstract void startContextMode(ContextModeCallback callback);

    /**
     * Finish the current context mode.
     */
    public abstract void finishContextMode();

    /**
     * Set the action bar into tabbed navigation mode.
     *
     * @see #addTab(Tab)
     * @see #insertTab(Tab, int)
     * @see #removeTab(Tab)
     * @see #removeTabAt(int)
     */
    public abstract void setTabNavigationMode();

    /**
     * Set the action bar into tabbed navigation mode.
     *
     * @param containerViewId Id of the container view where tab content fragments should appear.
     *
     * @see #addTab(Tab)
     * @see #insertTab(Tab, int)
     * @see #removeTab(Tab)
     * @see #removeTabAt(int)
     */
    public abstract void setTabNavigationMode(int containerViewId);

    /**
     * Create and return a new {@link Tab}.
     * This tab will not be included in the action bar until it is added.
     *
     * @return A new Tab
     *
     * @see #addTab(Tab)
     * @see #insertTab(Tab, int)
     */
    public abstract Tab newTab();

    /**
     * Add a tab for use in tabbed navigation mode. The tab will be added at the end of the list.
     *
     * @param tab Tab to add
     */
    public abstract void addTab(Tab tab);

    /**
     * Insert a tab for use in tabbed navigation mode. The tab will be inserted at
     * <code>position</code>.
     *
     * @param tab The tab to add
     * @param position The new position of the tab
     */
    public abstract void insertTab(Tab tab, int position);

    /**
     * Remove a tab from the action bar.
     *
     * @param tab The tab to remove
     */
    public abstract void removeTab(Tab tab);

    /**
     * Remove a tab from the action bar.
     *
     * @param position Position of the tab to remove
     */
    public abstract void removeTabAt(int position);

    /**
     * Select the specified tab. If it is not a child of this action bar it will be added.
     *
     * @param tab Tab to select
     */
    public abstract void selectTab(Tab tab);

    /**
     * Select the tab at <code>position</code>
     *
     * @param position Position of the tab to select
     */
    public abstract void selectTabAt(int position);

    /**
     * Represents a contextual mode of the Action Bar. Context modes can be used for
     * modal interactions with activity content and replace the normal Action Bar until finished.
     * Examples of good contextual modes include selection modes, search, content editing, etc.
     */
    public static abstract class ContextMode {
        /**
         * Set the title of the context mode. This method will have no visible effect if
         * a custom view has been set.
         * 
         * @param title Title string to set
         * 
         * @see #setCustomView(View)
         */
        public abstract void setTitle(CharSequence title);
        
        /**
         * Set the subtitle of the context mode. This method will have no visible effect if
         * a custom view has been set.
         * 
         * @param subtitle Subtitle string to set
         * 
         * @see #setCustomView(View)
         */
        public abstract void setSubtitle(CharSequence subtitle);
        
        /**
         * Set a custom view for this context mode. The custom view will take the place of
         * the title and subtitle. Useful for things like search boxes.
         *  
         * @param view Custom view to use in place of the title/subtitle.
         * 
         * @see #setTitle(CharSequence)
         * @see #setSubtitle(CharSequence)
         */
        public abstract void setCustomView(View view);
        
        /**
         * Invalidate the context mode and refresh menu content. The context mode's
         * {@link ContextModeCallback} will have its
         * {@link ContextModeCallback#onPrepareContextMode(ContextMode, Menu)} method called.
         * If it returns true the menu will be scanned for updated content and any relevant changes
         * will be reflected to the user.
         */
        public abstract void invalidate();
        
        /**
         * Finish and close this context mode. The context mode's {@link ContextModeCallback} will
         * have its {@link ContextModeCallback#onDestroyContextMode(ContextMode)} method called.
         */
        public abstract void finish();

        /**
         * Returns the menu of actions that this context mode presents.
         * @return The context mode's menu.
         */
        public abstract Menu getMenu();

        /**
         * Returns the current title of this context mode.
         * @return Title text
         */
        public abstract CharSequence getTitle();

        /**
         * Returns the current subtitle of this context mode.
         * @return Subtitle text
         */
        public abstract CharSequence getSubtitle();

        /**
         * Returns the current custom view for this context mode.
         * @return The current custom view
         */
        public abstract View getCustomView();
    }
    
    /**
     * Callback interface for ActionBar context modes. Supplied to
     * {@link ActionBar#startContextMode(ContextModeCallback)}, a ContextModeCallback
     * configures and handles events raised by a user's interaction with a context mode.
     * 
     * <p>A context mode's lifecycle is as follows:
     * <ul>
     * <li>{@link ContextModeCallback#onCreateContextMode(ContextMode, Menu)} once on initial
     * creation</li>
     * <li>{@link ContextModeCallback#onPrepareContextMode(ContextMode, Menu)} after creation
     * and any time the {@link ContextMode} is invalidated</li>
     * <li>{@link ContextModeCallback#onContextItemClicked(ContextMode, MenuItem)} any time a
     * contextual action button is clicked</li>
     * <li>{@link ContextModeCallback#onDestroyContextMode(ContextMode)} when the context mode
     * is closed</li>
     * </ul>
     */
    public interface ContextModeCallback {
        /**
         * Called when a context mode is first created. The menu supplied will be used to generate
         * action buttons for the context mode.
         * 
         * @param mode ContextMode being created
         * @param menu Menu used to populate contextual action buttons
         * @return true if the context mode should be created, false if entering this context mode
         *          should be aborted.
         */
        public boolean onCreateContextMode(ContextMode mode, Menu menu);
        
        /**
         * Called to refresh a context mode's action menu whenever it is invalidated.
         * 
         * @param mode ContextMode being prepared
         * @param menu Menu used to populate contextual action buttons
         * @return true if the menu or context mode was updated, false otherwise.
         */
        public boolean onPrepareContextMode(ContextMode mode, Menu menu);
        
        /**
         * Called to report a user click on a contextual action button.
         * 
         * @param mode The current ContextMode
         * @param item The item that was clicked
         * @return true if this callback handled the event, false if the standard MenuItem
         *          invocation should continue.
         */
        public boolean onContextItemClicked(ContextMode mode, MenuItem item);
        
        /**
         * Called when a context mode is about to be exited and destroyed.
         * 
         * @param mode The current ContextMode being destroyed
         */
        public void onDestroyContextMode(ContextMode mode);
    }
    
    /**
     * Callback interface for ActionBar navigation events. 
     */
    public interface NavigationCallback {
        /**
         * This method is called whenever a navigation item in your action bar
         * is selected.
         *    
         * @param itemPosition Position of the item clicked.
         * @param itemId ID of the item clicked.
         * @return True if the event was handled, false otherwise.
         */
        public boolean onNavigationItemSelected(int itemPosition, long itemId);
    }

    /**
     * A tab in the action bar.
     *
     * <p>Tabs manage the hiding and showing of {@link Fragment}s.
     */
    public static abstract class Tab {
        /**
         * An invalid position for a tab.
         *
         * @see #getPosition()
         */
        public static final int INVALID_POSITION = -1;

        /**
         * Return the current position of this tab in the action bar.
         *
         * @return Current position, or {@link #INVALID_POSITION} if this tab is not currently in
         *         the action bar.
         */
        public abstract int getPosition();

        /**
         * Return the icon associated with this tab.
         *
         * @return The tab's icon
         */
        public abstract Drawable getIcon();

        /**
         * Return the text of this tab.
         *
         * @return The tab's text
         */
        public abstract CharSequence getText();

        /**
         * Set the icon displayed on this tab.
         *
         * @param icon The drawable to use as an icon
         */
        public abstract void setIcon(Drawable icon);

        /**
         * Set the text displayed on this tab. Text may be truncated if there is not
         * room to display the entire string.
         *
         * @param text The text to display
         */
        public abstract void setText(CharSequence text);

        /**
         * Returns the fragment that will be shown when this tab is selected.
         *
         * @return Fragment associated with this tab
         */
        public abstract Fragment getFragment();

        /**
         * Set the fragment that will be shown when this tab is selected.
         *
         * @param fragment Fragment to associate with this tab
         */
        public abstract void setFragment(Fragment fragment);

        /**
         * Select this tab. Only valid if the tab has been added to the action bar.
         */
        public abstract void select();
    }
}
