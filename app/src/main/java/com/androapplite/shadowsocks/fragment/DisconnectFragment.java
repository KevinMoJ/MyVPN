package com.androapplite.shadowsocks.fragment;


import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.androapplite.shadowsocks.R;

/**
 * A simple {@link Fragment} subclass.
 */
public class DisconnectFragment extends DialogFragment implements View.OnClickListener{
    private OnDisconnectActionListener mListener;

    public DisconnectFragment() {
        // Required empty public constructor
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View v = inflater.inflate(R.layout.fragment_disconnect, container, false);
        v.findViewById(R.id.cancel_disconnect).setOnClickListener(this);
        v.findViewById(R.id.disconnect).setOnClickListener(this);
        return  v;
    }

    public interface OnDisconnectActionListener{
        void onCancel(DisconnectFragment disconnectFragment);
        void onDisconnect(DisconnectFragment disconnectFragment);
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof OnDisconnectActionListener) {
            mListener = (OnDisconnectActionListener) context;
        } else {
            throw new RuntimeException(context.toString()
                    + " must implement OnDisconnectActionListener");
        }
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        if (activity instanceof OnDisconnectActionListener) {
            mListener = (OnDisconnectActionListener) activity;
        } else {
            throw new RuntimeException(activity.toString()
                    + " must implement OnDisconnectActionListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    @Override
    public void onClick(View v) {
        if(mListener != null){
            dismiss();
            switch (v.getId()){
                case R.id.cancel_disconnect:
                    mListener.onCancel(this);
                    break;
                case R.id.disconnect:
                    mListener.onDisconnect(this);
                    break;
            }
        }
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Dialog dialog = super.onCreateDialog(savedInstanceState);
        dialog.requestWindowFeature(STYLE_NO_TITLE);
        return dialog;
    }
}
