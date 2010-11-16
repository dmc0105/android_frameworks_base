/*
 * Copyright (C) 2007 The Android Open Source Project
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

package android.preference;

import com.android.internal.util.XmlUtils;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import android.app.Fragment;
import android.app.FragmentBreadCrumbs;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.app.ListActivity;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.content.res.TypedArray;
import android.content.res.XmlResourceParser;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.Parcel;
import android.os.Parcelable;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.Xml;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * This is the base class for an activity to show a hierarchy of preferences
 * to the user.  Prior to {@link android.os.Build.VERSION_CODES#HONEYCOMB}
 * this class only allowed the display of a single set of preference; this
 * functionality should now be found in the new {@link PreferenceFragment}
 * class.  If you are using PreferenceActivity in its old mode, the documentation
 * there applies to the deprecated APIs here.
 *
 * <p>This activity shows one or more headers of preferences, each of with
 * is associated with a {@link PreferenceFragment} to display the preferences
 * of that header.  The actual layout and display of these associations can
 * however vary; currently there are two major approaches it may take:
 *
 * <ul>
 * <li>On a small screen it may display only the headers as a single list
 * when first launched.  Selecting one of the header items will re-launch
 * the activity with it only showing the PreferenceFragment of that header.
 * <li>On a large screen in may display both the headers and current
 * PreferenceFragment together as panes.  Selecting a header item switches
 * to showing the correct PreferenceFragment for that item.
 * </ul>
 *
 * <p>Subclasses of PreferenceActivity should implement
 * {@link #onBuildHeaders} to populate the header list with the desired
 * items.  Doing this implicitly switches the class into its new "headers
 * + fragments" mode rather than the old style of just showing a single
 * preferences list.
 *
 * <a name="SampleCode"></a>
 * <h3>Sample Code</h3>
 *
 * <p>The following sample code shows a simple preference activity that
 * has two different sets of preferences.  The implementation, consisting
 * of the activity itself as well as its two preference fragments is:</p>
 *
 * {@sample development/samples/ApiDemos/src/com/example/android/apis/preference/PreferenceWithHeaders.java
 *      activity}
 *
 * <p>The preference_headers resource describes the headers to be displayed
 * and the fragments associated with them.  It is:
 *
 * {@sample development/samples/ApiDemos/res/xml/preference_headers.xml headers}
 *
 * <p>The first header is shown by Prefs1Fragment, which populates itself
 * from the following XML resource:</p>
 *
 * {@sample development/samples/ApiDemos/res/xml/fragmented_preferences.xml preferences}
 *
 * <p>Note that this XML resource contains a preference screen holding another
 * fragment, the Prefs1FragmentInner implemented here.  This allows the user
 * to traverse down a hierarchy of preferences; pressing back will pop each
 * fragment off the stack to return to the previous preferences.
 *
 * <p>See {@link PreferenceFragment} for information on implementing the
 * fragments themselves.
 */
public abstract class PreferenceActivity extends ListActivity implements
        PreferenceManager.OnPreferenceTreeClickListener,
        PreferenceFragment.OnPreferenceStartFragmentCallback {
    private static final String TAG = "PreferenceActivity";

    // Constants for state save/restore
    private static final String HEADERS_TAG = ":android:headers";
    private static final String CUR_HEADER_TAG = ":android:cur_header";
    private static final String PREFERENCES_TAG = ":android:preferences";

    /**
     * When starting this activity, the invoking Intent can contain this extra
     * string to specify which fragment should be initially displayed.
     */
    public static final String EXTRA_SHOW_FRAGMENT = ":android:show_fragment";

    /**
     * When starting this activity and using {@link #EXTRA_SHOW_FRAGMENT},
     * this extra can also be specify to supply a Bundle of arguments to pass
     * to that fragment when it is instantiated during the initial creation
     * of PreferenceActivity.
     */
    public static final String EXTRA_SHOW_FRAGMENT_ARGUMENTS = ":android:show_fragment_args";

    /**
     * When starting this activity, the invoking Intent can contain this extra
     * boolean that the header list should not be displayed.  This is most often
     * used in conjunction with {@link #EXTRA_SHOW_FRAGMENT} to launch
     * the activity to display a specific fragment that the user has navigated
     * to.
     */
    public static final String EXTRA_NO_HEADERS = ":android:no_headers";

    private static final String BACK_STACK_PREFS = ":android:prefs";

    // extras that allow any preference activity to be launched as part of a wizard

    // show Back and Next buttons? takes boolean parameter
    // Back will then return RESULT_CANCELED and Next RESULT_OK
    private static final String EXTRA_PREFS_SHOW_BUTTON_BAR = "extra_prefs_show_button_bar";

    // add a Skip button?
    private static final String EXTRA_PREFS_SHOW_SKIP = "extra_prefs_show_skip";

    // specify custom text for the Back or Next buttons, or cause a button to not appear
    // at all by setting it to null
    private static final String EXTRA_PREFS_SET_NEXT_TEXT = "extra_prefs_set_next_text";
    private static final String EXTRA_PREFS_SET_BACK_TEXT = "extra_prefs_set_back_text";

    // --- State for new mode when showing a list of headers + prefs fragment

    private final ArrayList<Header> mHeaders = new ArrayList<Header>();

    private HeaderAdapter mAdapter;

    private FrameLayout mListFooter;

    private View mPrefsContainer;

    private FragmentBreadCrumbs mFragmentBreadCrumbs;

    private boolean mSinglePane;

    private Header mCurHeader;

    // --- State for old mode when showing a single preference list

    private PreferenceManager mPreferenceManager;

    private Bundle mSavedInstanceState;

    // --- Common state

    private Button mNextButton;

    /**
     * The starting request code given out to preference framework.
     */
    private static final int FIRST_REQUEST_CODE = 100;

    private static final int MSG_BIND_PREFERENCES = 1;
    private static final int MSG_BUILD_HEADERS = 2;
    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MSG_BIND_PREFERENCES: {
                    bindPreferences();
                } break;
                case MSG_BUILD_HEADERS: {
                    ArrayList<Header> oldHeaders = new ArrayList<Header>(mHeaders);
                    mHeaders.clear();
                    onBuildHeaders(mHeaders);
                    if (mAdapter != null) {
                        mAdapter.notifyDataSetChanged();
                    }
                    Header header = onGetNewHeader();
                    if (header != null && header.fragment != null) {
                        Header mappedHeader = findBestMatchingHeader(header, oldHeaders);
                        if (mappedHeader == null || mCurHeader != mappedHeader) {
                            switchToHeader(header);
                        }
                    } else if (mCurHeader != null) {
                        Header mappedHeader = findBestMatchingHeader(mCurHeader, mHeaders);
                        if (mappedHeader != null) {
                            setSelectedHeader(mappedHeader);
                        }
                    }
                } break;
            }
        }
    };

    private static class HeaderAdapter extends ArrayAdapter<Header> {
        private static class HeaderViewHolder {
            ImageView icon;
            TextView title;
            TextView summary;
        }

        private LayoutInflater mInflater;

        public HeaderAdapter(Context context, List<Header> objects) {
            super(context, 0, objects);
            mInflater = (LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            HeaderViewHolder holder;
            View view;

            if (convertView == null) {
                view = mInflater.inflate(com.android.internal.R.layout.preference_header_item,
                        parent, false);
                holder = new HeaderViewHolder();
                holder.icon = (ImageView) view.findViewById(com.android.internal.R.id.icon);
                holder.title = (TextView) view.findViewById(com.android.internal.R.id.title);
                holder.summary = (TextView) view.findViewById(com.android.internal.R.id.summary);
                view.setTag(holder);
            } else {
                view = convertView;
                holder = (HeaderViewHolder) view.getTag();
            }

            // All view fields must be updated every time, because the view may be recycled 
            Header header = getItem(position);
            holder.icon.setImageResource(header.iconRes);
            holder.title.setText(header.title);
            if (TextUtils.isEmpty(header.summary)) {
                holder.summary.setVisibility(View.GONE);
            } else {
                holder.summary.setVisibility(View.VISIBLE);
                holder.summary.setText(header.summary);
            }

            return view;
        }
    }

    /**
     * Default value for {@link Header#id Header.id} indicating that no
     * identifier value is set.  All other values (including those below -1)
     * are valid.
     */
    public static final long HEADER_ID_UNDEFINED = -1;
    
    /**
     * Description of a single Header item that the user can select.
     */
    public static final class Header implements Parcelable {
        /**
         * Identifier for this header, to correlate with a new list when
         * it is updated.  The default value is
         * {@link PreferenceActivity#HEADER_ID_UNDEFINED}, meaning no id.
         * @attr ref android.R.styleable#PreferenceHeader_id
         */
        public long id = HEADER_ID_UNDEFINED;

        /**
         * Title of the header that is shown to the user.
         * @attr ref android.R.styleable#PreferenceHeader_title
         */
        public CharSequence title;

        /**
         * Optional summary describing what this header controls.
         * @attr ref android.R.styleable#PreferenceHeader_summary
         */
        public CharSequence summary;

        /**
         * Optional text to show as the title in the bread crumb.
         * @attr ref android.R.styleable#PreferenceHeader_breadCrumbTitle
         */
        public CharSequence breadCrumbTitle;

        /**
         * Optional text to show as the short title in the bread crumb.
         * @attr ref android.R.styleable#PreferenceHeader_breadCrumbShortTitle
         */
        public CharSequence breadCrumbShortTitle;

        /**
         * Optional icon resource to show for this header.
         * @attr ref android.R.styleable#PreferenceHeader_icon
         */
        public int iconRes;

        /**
         * Full class name of the fragment to display when this header is
         * selected.
         * @attr ref android.R.styleable#PreferenceHeader_fragment
         */
        public String fragment;

        /**
         * Optional arguments to supply to the fragment when it is
         * instantiated.
         */
        public Bundle fragmentArguments;

        /**
         * Intent to launch when the preference is selected.
         */
        public Intent intent;

        /**
         * Optional additional data for use by subclasses of PreferenceActivity.
         */
        public Bundle extras;

        public Header() {
        }

        @Override
        public int describeContents() {
            return 0;
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            dest.writeLong(id);
            TextUtils.writeToParcel(title, dest, flags);
            TextUtils.writeToParcel(summary, dest, flags);
            TextUtils.writeToParcel(breadCrumbTitle, dest, flags);
            TextUtils.writeToParcel(breadCrumbShortTitle, dest, flags);
            dest.writeInt(iconRes);
            dest.writeString(fragment);
            dest.writeBundle(fragmentArguments);
            if (intent != null) {
                dest.writeInt(1);
                intent.writeToParcel(dest, flags);
            } else {
                dest.writeInt(0);
            }
            dest.writeBundle(extras);
        }

        public void readFromParcel(Parcel in) {
            id = in.readLong();
            title = TextUtils.CHAR_SEQUENCE_CREATOR.createFromParcel(in);
            summary = TextUtils.CHAR_SEQUENCE_CREATOR.createFromParcel(in);
            breadCrumbTitle = TextUtils.CHAR_SEQUENCE_CREATOR.createFromParcel(in);
            breadCrumbShortTitle = TextUtils.CHAR_SEQUENCE_CREATOR.createFromParcel(in);
            iconRes = in.readInt();
            fragment = in.readString();
            fragmentArguments = in.readBundle();
            if (in.readInt() != 0) {
                intent = Intent.CREATOR.createFromParcel(in);
            }
            extras = in.readBundle();
        }

        Header(Parcel in) {
            readFromParcel(in);
        }

        public static final Creator<Header> CREATOR = new Creator<Header>() {
            public Header createFromParcel(Parcel source) {
                return new Header(source);
            }
            public Header[] newArray(int size) {
                return new Header[size];
            }
        };
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(com.android.internal.R.layout.preference_list_content);

        mListFooter = (FrameLayout)findViewById(com.android.internal.R.id.list_footer);
        mPrefsContainer = findViewById(com.android.internal.R.id.prefs);
        boolean hidingHeaders = onIsHidingHeaders();
        mSinglePane = hidingHeaders || !onIsMultiPane();
        String initialFragment = getIntent().getStringExtra(EXTRA_SHOW_FRAGMENT);
        Bundle initialArguments = getIntent().getBundleExtra(EXTRA_SHOW_FRAGMENT_ARGUMENTS);

        if (savedInstanceState != null) {
            // We are restarting from a previous saved state; used that to
            // initialize, instead of starting fresh.
            ArrayList<Header> headers = savedInstanceState.getParcelableArrayList(HEADERS_TAG);
            if (headers != null) {
                mHeaders.addAll(headers);
                int curHeader = savedInstanceState.getInt(CUR_HEADER_TAG,
                        (int)HEADER_ID_UNDEFINED);
                if (curHeader >= 0 && curHeader < mHeaders.size()) {
                    setSelectedHeader(mHeaders.get(curHeader));
                }
            }

        } else {
            if (initialFragment != null && mSinglePane) {
                // If we are just showing a fragment, we want to run in
                // new fragment mode, but don't need to compute and show
                // the headers.
                switchToHeader(initialFragment, initialArguments);

            } else {
                // We need to try to build the headers.
                onBuildHeaders(mHeaders);

                // If there are headers, then at this point we need to show
                // them and, depending on the screen, we may also show in-line
                // the currently selected preference fragment.
                if (mHeaders.size() > 0) {
                    if (!mSinglePane) {
                        if (initialFragment == null) {
                            Header h = onGetInitialHeader();
                            switchToHeader(h);
                        } else {
                            switchToHeader(initialFragment, initialArguments);
                        }
                    }
                }
            }
        }

        // The default configuration is to only show the list view.  Adjust
        // visibility for other configurations.
        if (initialFragment != null && mSinglePane) {
            // Single pane, showing just a prefs fragment.
            findViewById(com.android.internal.R.id.headers).setVisibility(View.GONE);
            mPrefsContainer.setVisibility(View.VISIBLE);
        } else if (mHeaders.size() > 0) {
            mAdapter = new HeaderAdapter(this, mHeaders);
            setListAdapter(mAdapter);
            if (!mSinglePane) {
                // Multi-pane.
                getListView().setChoiceMode(AbsListView.CHOICE_MODE_SINGLE);
                if (mCurHeader != null) {
                    setSelectedHeader(mCurHeader);
                }
                mPrefsContainer.setVisibility(View.VISIBLE);
            }
        } else {
            // If there are no headers, we are in the old "just show a screen
            // of preferences" mode.
            setContentView(com.android.internal.R.layout.preference_list_content_single);
            mListFooter = (FrameLayout) findViewById(com.android.internal.R.id.list_footer);
            mPrefsContainer = findViewById(com.android.internal.R.id.prefs);
            mPreferenceManager = new PreferenceManager(this, FIRST_REQUEST_CODE);
            mPreferenceManager.setOnPreferenceTreeClickListener(this);
        }

        getListView().setScrollBarStyle(View.SCROLLBARS_INSIDE_OVERLAY);

        // see if we should show Back/Next buttons
        Intent intent = getIntent();
        if (intent.getBooleanExtra(EXTRA_PREFS_SHOW_BUTTON_BAR, false)) {

            findViewById(com.android.internal.R.id.button_bar).setVisibility(View.VISIBLE);

            Button backButton = (Button)findViewById(com.android.internal.R.id.back_button);
            backButton.setOnClickListener(new OnClickListener() {
                public void onClick(View v) {
                    setResult(RESULT_CANCELED);
                    finish();
                }
            });
            Button skipButton = (Button)findViewById(com.android.internal.R.id.skip_button);
            skipButton.setOnClickListener(new OnClickListener() {
                public void onClick(View v) {
                    setResult(RESULT_OK);
                    finish();
                }
            });
            mNextButton = (Button)findViewById(com.android.internal.R.id.next_button);
            mNextButton.setOnClickListener(new OnClickListener() {
                public void onClick(View v) {
                    setResult(RESULT_OK);
                    finish();
                }
            });

            // set our various button parameters
            if (intent.hasExtra(EXTRA_PREFS_SET_NEXT_TEXT)) {
                String buttonText = intent.getStringExtra(EXTRA_PREFS_SET_NEXT_TEXT);
                if (TextUtils.isEmpty(buttonText)) {
                    mNextButton.setVisibility(View.GONE);
                }
                else {
                    mNextButton.setText(buttonText);
                }
            }
            if (intent.hasExtra(EXTRA_PREFS_SET_BACK_TEXT)) {
                String buttonText = intent.getStringExtra(EXTRA_PREFS_SET_BACK_TEXT);
                if (TextUtils.isEmpty(buttonText)) {
                    backButton.setVisibility(View.GONE);
                }
                else {
                    backButton.setText(buttonText);
                }
            }
            if (intent.getBooleanExtra(EXTRA_PREFS_SHOW_SKIP, false)) {
                skipButton.setVisibility(View.VISIBLE);
            }
        }
    }

    /**
     * Returns true if this activity is currently showing the header list.
     */
    public boolean hasHeaders() {
        return getListView().getVisibility() == View.VISIBLE
                && mPreferenceManager == null;
    }

    /**
     * Returns true if this activity is showing multiple panes -- the headers
     * and a preference fragment.
     */
    public boolean isMultiPane() {
        return hasHeaders() && mPrefsContainer.getVisibility() == View.VISIBLE;
    }

    /**
     * Called to determine if the activity should run in multi-pane mode.
     * The default implementation returns true if the screen is large
     * enough.
     */
    public boolean onIsMultiPane() {
        Configuration config = getResources().getConfiguration();
        if ((config.screenLayout&Configuration.SCREENLAYOUT_SIZE_MASK)
                == Configuration.SCREENLAYOUT_SIZE_XLARGE) {
            return true;
        }
        if ((config.screenLayout&Configuration.SCREENLAYOUT_SIZE_MASK)
                == Configuration.SCREENLAYOUT_SIZE_LARGE
                && config.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            return true;
        }
        return false;
    }

    /**
     * Called to determine whether the header list should be hidden.
     * The default implementation returns the
     * value given in {@link #EXTRA_NO_HEADERS} or false if it is not supplied.
     * This is set to false, for example, when the activity is being re-launched
     * to show a particular preference activity.
     */
    public boolean onIsHidingHeaders() {
        return getIntent().getBooleanExtra(EXTRA_NO_HEADERS, false);
    }

    /**
     * Called to determine the initial header to be shown.  The default
     * implementation simply returns the fragment of the first header.  Note
     * that the returned Header object does not actually need to exist in
     * your header list -- whatever its fragment is will simply be used to
     * show for the initial UI.
     */
    public Header onGetInitialHeader() {
        return mHeaders.get(0);
    }

    /**
     * Called after the header list has been updated ({@link #onBuildHeaders}
     * has been called and returned due to {@link #invalidateHeaders()}) to
     * specify the header that should now be selected.  The default implementation
     * returns null to keep whatever header is currently selected.
     */
    public Header onGetNewHeader() {
        return null;
    }

    /**
     * Called when the activity needs its list of headers build.  By
     * implementing this and adding at least one item to the list, you
     * will cause the activity to run in its modern fragment mode.  Note
     * that this function may not always be called; for example, if the
     * activity has been asked to display a particular fragment without
     * the header list, there is no need to build the headers.
     *
     * <p>Typical implementations will use {@link #loadHeadersFromResource}
     * to fill in the list from a resource.
     *
     * @param target The list in which to place the headers.
     */
    public void onBuildHeaders(List<Header> target) {
    }

    /**
     * Call when you need to change the headers being displayed.  Will result
     * in onBuildHeaders() later being called to retrieve the new list.
     */
    public void invalidateHeaders() {
        if (!mHandler.hasMessages(MSG_BUILD_HEADERS)) {
            mHandler.sendEmptyMessage(MSG_BUILD_HEADERS);
        }
    }

    /**
     * Parse the given XML file as a header description, adding each
     * parsed Header into the target list.
     *
     * @param resid The XML resource to load and parse.
     * @param target The list in which the parsed headers should be placed.
     */
    public void loadHeadersFromResource(int resid, List<Header> target) {
        XmlResourceParser parser = null;
        try {
            parser = getResources().getXml(resid);
            AttributeSet attrs = Xml.asAttributeSet(parser);

            int type;
            while ((type=parser.next()) != XmlPullParser.END_DOCUMENT
                    && type != XmlPullParser.START_TAG) {
            }

            String nodeName = parser.getName();
            if (!"preference-headers".equals(nodeName)) {
                throw new RuntimeException(
                        "XML document must start with <preference-headers> tag; found"
                        + nodeName + " at " + parser.getPositionDescription());
            }

            Bundle curBundle = null;

            final int outerDepth = parser.getDepth();
            while ((type=parser.next()) != XmlPullParser.END_DOCUMENT
                   && (type != XmlPullParser.END_TAG || parser.getDepth() > outerDepth)) {
                if (type == XmlPullParser.END_TAG || type == XmlPullParser.TEXT) {
                    continue;
                }

                nodeName = parser.getName();
                if ("header".equals(nodeName)) {
                    Header header = new Header();

                    TypedArray sa = getResources().obtainAttributes(attrs,
                            com.android.internal.R.styleable.PreferenceHeader);
                    header.id = sa.getResourceId(
                            com.android.internal.R.styleable.PreferenceHeader_id,
                            (int)HEADER_ID_UNDEFINED);
                    header.title = sa.getText(
                            com.android.internal.R.styleable.PreferenceHeader_title);
                    header.summary = sa.getText(
                            com.android.internal.R.styleable.PreferenceHeader_summary);
                    header.breadCrumbTitle = sa.getText(
                            com.android.internal.R.styleable.PreferenceHeader_breadCrumbTitle);
                    header.breadCrumbShortTitle = sa.getText(
                            com.android.internal.R.styleable.PreferenceHeader_breadCrumbShortTitle);
                    header.iconRes = sa.getResourceId(
                            com.android.internal.R.styleable.PreferenceHeader_icon, 0);
                    header.fragment = sa.getString(
                            com.android.internal.R.styleable.PreferenceHeader_fragment);
                    sa.recycle();

                    if (curBundle == null) {
                        curBundle = new Bundle();
                    }

                    final int innerDepth = parser.getDepth();
                    while ((type=parser.next()) != XmlPullParser.END_DOCUMENT
                           && (type != XmlPullParser.END_TAG || parser.getDepth() > innerDepth)) {
                        if (type == XmlPullParser.END_TAG || type == XmlPullParser.TEXT) {
                            continue;
                        }

                        String innerNodeName = parser.getName();
                        if (innerNodeName.equals("extra")) {
                            getResources().parseBundleExtra("extra", attrs, curBundle);
                            XmlUtils.skipCurrentTag(parser);

                        } else if (innerNodeName.equals("intent")) {
                            header.intent = Intent.parseIntent(getResources(), parser, attrs);

                        } else {
                            XmlUtils.skipCurrentTag(parser);
                        }
                    }

                    if (curBundle.size() > 0) {
                        header.fragmentArguments = curBundle;
                        curBundle = null;
                    }

                    target.add(header);
                } else {
                    XmlUtils.skipCurrentTag(parser);
                }
            }

        } catch (XmlPullParserException e) {
            throw new RuntimeException("Error parsing headers", e);
        } catch (IOException e) {
            throw new RuntimeException("Error parsing headers", e);
        } finally {
            if (parser != null) parser.close();
        }

    }

    /**
     * Set a footer that should be shown at the bottom of the header list.
     */
    public void setListFooter(View view) {
        mListFooter.removeAllViews();
        mListFooter.addView(view, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.WRAP_CONTENT));
    }

    @Override
    protected void onStop() {
        super.onStop();

        if (mPreferenceManager != null) {
            mPreferenceManager.dispatchActivityStop();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (mPreferenceManager != null) {
            mPreferenceManager.dispatchActivityDestroy();
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        if (mHeaders.size() > 0) {
            outState.putParcelableArrayList(HEADERS_TAG, mHeaders);
            if (mCurHeader != null) {
                int index = mHeaders.indexOf(mCurHeader);
                if (index >= 0) {
                    outState.putInt(CUR_HEADER_TAG, index);
                }
            }
        }

        if (mPreferenceManager != null) {
            final PreferenceScreen preferenceScreen = getPreferenceScreen();
            if (preferenceScreen != null) {
                Bundle container = new Bundle();
                preferenceScreen.saveHierarchyState(container);
                outState.putBundle(PREFERENCES_TAG, container);
            }
        }
    }

    @Override
    protected void onRestoreInstanceState(Bundle state) {
        if (mPreferenceManager != null) {
            Bundle container = state.getBundle(PREFERENCES_TAG);
            if (container != null) {
                final PreferenceScreen preferenceScreen = getPreferenceScreen();
                if (preferenceScreen != null) {
                    preferenceScreen.restoreHierarchyState(container);
                    mSavedInstanceState = state;
                    return;
                }
            }
        }

        // Only call this if we didn't save the instance state for later.
        // If we did save it, it will be restored when we bind the adapter.
        super.onRestoreInstanceState(state);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (mPreferenceManager != null) {
            mPreferenceManager.dispatchActivityResult(requestCode, resultCode, data);
        }
    }

    @Override
    public void onContentChanged() {
        super.onContentChanged();

        if (mPreferenceManager != null) {
            postBindPreferences();
        }
    }

    @Override
    protected void onListItemClick(ListView l, View v, int position, long id) {
        super.onListItemClick(l, v, position, id);

        if (mAdapter != null) {
            onHeaderClick(mHeaders.get(position), position);
        }
    }

    /**
     * Called when the user selects an item in the header list.  The default
     * implementation will call either {@link #startWithFragment(String, Bundle, Fragment, int)}
     * or {@link #switchToHeader(Header)} as appropriate.
     *
     * @param header The header that was selected.
     * @param position The header's position in the list.
     */
    public void onHeaderClick(Header header, int position) {
        if (header.fragment != null) {
            if (mSinglePane) {
                startWithFragment(header.fragment, header.fragmentArguments, null, 0);
            } else {
                switchToHeader(header);
            }
        } else if (header.intent != null) {
            startActivity(header.intent);
        }
    }

    /**
     * Start a new instance of this activity, showing only the given
     * preference fragment.  When launched in this mode, the header list
     * will be hidden and the given preference fragment will be instantiated
     * and fill the entire activity.
     *
     * @param fragmentName The name of the fragment to display.
     * @param args Optional arguments to supply to the fragment.
     */
    public void startWithFragment(String fragmentName, Bundle args,
            Fragment resultTo, int resultRequestCode) {
        Intent intent = new Intent(Intent.ACTION_MAIN);
        intent.setClass(this, getClass());
        intent.putExtra(EXTRA_SHOW_FRAGMENT, fragmentName);
        intent.putExtra(EXTRA_SHOW_FRAGMENT_ARGUMENTS, args);
        intent.putExtra(EXTRA_NO_HEADERS, true);
        if (resultTo == null) {
            startActivity(intent);
        } else {
            resultTo.startActivityForResult(intent, resultRequestCode);
        }
    }

    /**
     * Change the base title of the bread crumbs for the current preferences.
     * This will normally be called for you.  See
     * {@link android.app.FragmentBreadCrumbs} for more information.
     */
    public void showBreadCrumbs(CharSequence title, CharSequence shortTitle) {
        if (mFragmentBreadCrumbs == null) {
            mFragmentBreadCrumbs = new FragmentBreadCrumbs(this);
            mFragmentBreadCrumbs.setActivity(this);
            getActionBar().setCustomNavigationMode(mFragmentBreadCrumbs);
        }
        mFragmentBreadCrumbs.setTitle(title, shortTitle);
    }

    void setSelectedHeader(Header header) {
        mCurHeader = header;
        int index = mHeaders.indexOf(header);
        if (index >= 0) {
            getListView().setItemChecked(index, true);
        } else {
            getListView().clearChoices();
        }
        if (header != null) {
            CharSequence title = header.breadCrumbTitle;
            if (title == null) title = header.title;
            if (title == null) title = getTitle();
            showBreadCrumbs(title, header.breadCrumbShortTitle);
        } else {
            showBreadCrumbs(getTitle(), null);
        }
    }

    private void switchToHeaderInner(String fragmentName, Bundle args, int direction) {
        getFragmentManager().popBackStack(BACK_STACK_PREFS,
                FragmentManager.POP_BACK_STACK_INCLUSIVE);
        Fragment f = Fragment.instantiate(this, fragmentName, args);
        FragmentTransaction transaction = getFragmentManager().openTransaction();
        transaction.setTransition(direction == 0 ? FragmentTransaction.TRANSIT_NONE
                : direction > 0 ? FragmentTransaction.TRANSIT_FRAGMENT_NEXT
                        : FragmentTransaction.TRANSIT_FRAGMENT_PREV);
        transaction.replace(com.android.internal.R.id.prefs, f);
        transaction.commit();
    }

    /**
     * When in two-pane mode, switch the fragment pane to show the given
     * preference fragment.
     *
     * @param fragmentName The name of the fragment to display.
     * @param args Optional arguments to supply to the fragment.
     */
    public void switchToHeader(String fragmentName, Bundle args) {
        setSelectedHeader(null);
        switchToHeaderInner(fragmentName, args, 0);
    }

    /**
     * When in two-pane mode, switch to the fragment pane to show the given
     * preference fragment.
     *
     * @param header The new header to display.
     */
    public void switchToHeader(Header header) {
        if (mCurHeader == header) {
            // This is the header we are currently displaying.  Just make sure
            // to pop the stack up to its root state.
            getFragmentManager().popBackStack(BACK_STACK_PREFS,
                    FragmentManager.POP_BACK_STACK_INCLUSIVE);
        } else {
            int direction = mHeaders.indexOf(header) - mHeaders.indexOf(mCurHeader);
            switchToHeaderInner(header.fragment, header.fragmentArguments, direction);
            setSelectedHeader(header);
        }
    }

    Header findBestMatchingHeader(Header cur, ArrayList<Header> from) {
        ArrayList<Header> matches = new ArrayList<Header>();
        for (int j=0; j<from.size(); j++) {
            Header oh = from.get(j);
            if (cur == oh || (cur.id != HEADER_ID_UNDEFINED && cur.id == oh.id)) {
                // Must be this one.
                matches.clear();
                matches.add(oh);
                break;
            }
            if (cur.fragment != null) {
                if (cur.fragment.equals(oh.fragment)) {
                    matches.add(oh);
                }
            } else if (cur.intent != null) {
                if (cur.intent.equals(oh.intent)) {
                    matches.add(oh);
                }
            } else if (cur.title != null) {
                if (cur.title.equals(oh.title)) {
                    matches.add(oh);
                }
            }
        }
        final int NM = matches.size();
        if (NM == 1) {
            return matches.get(0);
        } else if (NM > 1) {
            for (int j=0; j<NM; j++) {
                Header oh = matches.get(j);
                if (cur.fragmentArguments != null &&
                        cur.fragmentArguments.equals(oh.fragmentArguments)) {
                    return oh;
                }
                if (cur.extras != null && cur.extras.equals(oh.extras)) {
                    return oh;
                }
                if (cur.title != null && cur.title.equals(oh.title)) {
                    return oh;
                }
            }
        }
        return null;
    }

    /**
     * Start a new fragment.
     *
     * @param fragment The fragment to start
     * @param push If true, the current fragment will be pushed onto the back stack.  If false,
     * the current fragment will be replaced.
     */
    public void startPreferenceFragment(Fragment fragment, boolean push) {
        FragmentTransaction transaction = getFragmentManager().openTransaction();
        transaction.replace(com.android.internal.R.id.prefs, fragment);
        if (push) {
            transaction.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN);
            transaction.addToBackStack(BACK_STACK_PREFS);
        } else {
            transaction.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_NEXT);
        }
        transaction.commit();
    }

    /**
     * Start a new fragment containing a preference panel.  If the prefences
     * are being displayed in multi-pane mode, the given fragment class will
     * be instantiated and placed in the appropriate pane.  If running in
     * single-pane mode, a new activity will be launched in which to show the
     * fragment.
     * 
     * @param fragmentClass Full name of the class implementing the fragment.
     * @param args Any desired arguments to supply to the fragment.
     * @param titleRes Optional resource identifier of the title of this
     * fragment.
     * @param titleText Optional text of the title of this fragment.
     * @param resultTo Optional fragment that result data should be sent to.
     * If non-null, resultTo.onActivityResult() will be called when this
     * preference panel is done.  The launched panel must use
     * {@link #finishPreferencePanel(Fragment, int, Intent)} when done.
     * @param resultRequestCode If resultTo is non-null, this is the caller's
     * request code to be received with the resut.
     */
    public void startPreferencePanel(String fragmentClass, Bundle args, int titleRes,
            CharSequence titleText, Fragment resultTo, int resultRequestCode) {
        if (mSinglePane) {
            startWithFragment(fragmentClass, args, resultTo, resultRequestCode);
        } else {
            Fragment f = Fragment.instantiate(this, fragmentClass, args);
            if (resultTo != null) {
                f.setTargetFragment(resultTo, resultRequestCode);
            }
            FragmentTransaction transaction = getFragmentManager().openTransaction();
            transaction.replace(com.android.internal.R.id.prefs, f);
            if (titleRes != 0) {
                transaction.setBreadCrumbTitle(titleRes);
            } else if (titleText != null) {
                transaction.setBreadCrumbTitle(titleText);
            }
            transaction.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN);
            transaction.addToBackStack(BACK_STACK_PREFS);
            transaction.commit();
        }
    }
    
    /**
     * Called by a preference panel fragment to finish itself.
     * 
     * @param caller The fragment that is asking to be finished.
     * @param resultCode Optional result code to send back to the original
     * launching fragment.
     * @param resultData Optional result data to send back to the original
     * launching fragment.
     */
    public void finishPreferencePanel(Fragment caller, int resultCode, Intent resultData) {
        if (mSinglePane) {
            setResult(resultCode, resultData);
            finish();
        } else {
            // XXX be smarter about popping the stack.
            onBackPressed();
            if (caller != null) {
                if (caller.getTargetFragment() != null) {
                    caller.getTargetFragment().onActivityResult(caller.getTargetRequestCode(),
                            resultCode, resultData);
                }
            }
        }
    }
    
    @Override
    public boolean onPreferenceStartFragment(PreferenceFragment caller, Preference pref) {
        startPreferencePanel(pref.getFragment(), pref.getExtras(), 0, pref.getTitle(), null, 0);
        return true;
    }

    /**
     * Posts a message to bind the preferences to the list view.
     * <p>
     * Binding late is preferred as any custom preference types created in
     * {@link #onCreate(Bundle)} are able to have their views recycled.
     */
    private void postBindPreferences() {
        if (mHandler.hasMessages(MSG_BIND_PREFERENCES)) return;
        mHandler.obtainMessage(MSG_BIND_PREFERENCES).sendToTarget();
    }

    private void bindPreferences() {
        final PreferenceScreen preferenceScreen = getPreferenceScreen();
        if (preferenceScreen != null) {
            preferenceScreen.bind(getListView());
            if (mSavedInstanceState != null) {
                super.onRestoreInstanceState(mSavedInstanceState);
                mSavedInstanceState = null;
            }
        }
    }

    /**
     * Returns the {@link PreferenceManager} used by this activity.
     * @return The {@link PreferenceManager}.
     *
     * @deprecated This function is not relevant for a modern fragment-based
     * PreferenceActivity.
     */
    @Deprecated
    public PreferenceManager getPreferenceManager() {
        return mPreferenceManager;
    }

    private void requirePreferenceManager() {
        if (mPreferenceManager == null) {
            if (mAdapter == null) {
                throw new RuntimeException("This should be called after super.onCreate.");
            }
            throw new RuntimeException(
                    "Modern two-pane PreferenceActivity requires use of a PreferenceFragment");
        }
    }

    /**
     * Sets the root of the preference hierarchy that this activity is showing.
     *
     * @param preferenceScreen The root {@link PreferenceScreen} of the preference hierarchy.
     *
     * @deprecated This function is not relevant for a modern fragment-based
     * PreferenceActivity.
     */
    @Deprecated
    public void setPreferenceScreen(PreferenceScreen preferenceScreen) {
        requirePreferenceManager();

        if (mPreferenceManager.setPreferences(preferenceScreen) && preferenceScreen != null) {
            postBindPreferences();
            CharSequence title = getPreferenceScreen().getTitle();
            // Set the title of the activity
            if (title != null) {
                setTitle(title);
            }
        }
    }

    /**
     * Gets the root of the preference hierarchy that this activity is showing.
     *
     * @return The {@link PreferenceScreen} that is the root of the preference
     *         hierarchy.
     *
     * @deprecated This function is not relevant for a modern fragment-based
     * PreferenceActivity.
     */
    @Deprecated
    public PreferenceScreen getPreferenceScreen() {
        if (mPreferenceManager != null) {
            return mPreferenceManager.getPreferenceScreen();
        }
        return null;
    }

    /**
     * Adds preferences from activities that match the given {@link Intent}.
     *
     * @param intent The {@link Intent} to query activities.
     *
     * @deprecated This function is not relevant for a modern fragment-based
     * PreferenceActivity.
     */
    @Deprecated
    public void addPreferencesFromIntent(Intent intent) {
        requirePreferenceManager();

        setPreferenceScreen(mPreferenceManager.inflateFromIntent(intent, getPreferenceScreen()));
    }

    /**
     * Inflates the given XML resource and adds the preference hierarchy to the current
     * preference hierarchy.
     *
     * @param preferencesResId The XML resource ID to inflate.
     *
     * @deprecated This function is not relevant for a modern fragment-based
     * PreferenceActivity.
     */
    @Deprecated
    public void addPreferencesFromResource(int preferencesResId) {
        requirePreferenceManager();

        setPreferenceScreen(mPreferenceManager.inflateFromResource(this, preferencesResId,
                getPreferenceScreen()));
    }

    /**
     * {@inheritDoc}
     *
     * @deprecated This function is not relevant for a modern fragment-based
     * PreferenceActivity.
     */
    @Deprecated
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {
        return false;
    }

    /**
     * Finds a {@link Preference} based on its key.
     *
     * @param key The key of the preference to retrieve.
     * @return The {@link Preference} with the key, or null.
     * @see PreferenceGroup#findPreference(CharSequence)
     *
     * @deprecated This function is not relevant for a modern fragment-based
     * PreferenceActivity.
     */
    @Deprecated
    public Preference findPreference(CharSequence key) {

        if (mPreferenceManager == null) {
            return null;
        }

        return mPreferenceManager.findPreference(key);
    }

    @Override
    protected void onNewIntent(Intent intent) {
        if (mPreferenceManager != null) {
            mPreferenceManager.dispatchNewIntent(intent);
        }
    }

    // give subclasses access to the Next button
    /** @hide */
    protected boolean hasNextButton() {
        return mNextButton != null;
    }
    /** @hide */
    protected Button getNextButton() {
        return mNextButton;
    }
}
