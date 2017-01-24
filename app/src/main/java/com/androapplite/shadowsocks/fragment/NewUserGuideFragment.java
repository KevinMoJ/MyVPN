package com.androapplite.shadowsocks.fragment;


import android.content.res.TypedArray;
import android.os.Bundle;
import android.support.annotation.DrawableRes;
import android.support.annotation.IntDef;
import android.support.annotation.StringRes;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.androapplite.vpn3.R;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link NewUserGuideFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class NewUserGuideFragment extends Fragment {

    private static final String RESOURCE_IDS = "resource_ids";

    public static final int IMAGE = 0;
    public static final int TITLE = 1;
    public static final int EXPLAIN = 2;
//    @IntDef({IMAGE, TITLE, EXPLAIN})
//    @Retention(RetentionPolicy.SOURCE)
//    public @interface USER_GUIDE_RESOURCE_ID {}


    public NewUserGuideFragment() {
        // Required empty public constructor
    }


    public static NewUserGuideFragment newInstance(int[] userGuideResourceIds){
        NewUserGuideFragment fragment = new NewUserGuideFragment();
        Bundle args = new Bundle();
        args.putIntArray(RESOURCE_IDS, userGuideResourceIds);
        fragment.setArguments(args);
        return fragment;
    }

//    @Override
//    public void onCreate(Bundle savedInstanceState) {
//        super.onCreate(savedInstanceState);
//        if (getArguments() != null) {
//            mParam1 = getArguments().getString(ARG_PARAM1);
//            mParam2 = getArguments().getString(ARG_PARAM2);
//        }
//    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View rootView = inflater.inflate(R.layout.fragment_new_user_guide, container, false);
        Bundle arguments = getArguments();
        if(arguments != null){
            int[] resourceIds = arguments.getIntArray(RESOURCE_IDS);
            if(resourceIds != null){
                @DrawableRes int imageResourceId = resourceIds[IMAGE];
                ImageView userGuideImage = (ImageView)rootView.findViewById(R.id.user_guide_image);
                userGuideImage.setImageResource(imageResourceId);

                @StringRes int titleResourceId = resourceIds[TITLE];
                TextView titleTextView = (TextView)rootView.findViewById(R.id.user_guide_title);
                titleTextView.setText(titleResourceId);

                @StringRes int explainResourceId = resourceIds[EXPLAIN];
                TextView explainTextView = (TextView)rootView.findViewById(R.id.user_guide_explain);
                explainTextView.setText(explainResourceId);
            }

        }
        return rootView;
    }

}
