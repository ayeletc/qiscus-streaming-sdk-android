package com.qiscus.streaming.ui.fragment;

import android.app.Dialog;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;

import com.qiscus.streaming.R;
import com.qiscus.streaming.data.Resolution;
import com.qiscus.streaming.ui.activity.QiscusStreamActivity;

import java.util.ArrayList;

/**
 * Created by fitra on 12/04/18.
 */

public class CameraResolutionsFragment extends DialogFragment implements AdapterView.OnItemClickListener {
    private static final String CAMERA_RESOLUTIONS = "CAMERA_RESOLUTIONS";
    private static final String SELECTED_SIZE_WIDTH = "SELECTED_SIZE_WIDTH";
    private static final String SELECTED_SIZE_HEIGHT = "SELECTED_SIZE_HEIGHT";

    private Dialog dialog;
    private ListView cameraResolutionsListView;
    private CameResolutionsAdapter resolutionAdapter = new CameResolutionsAdapter();

    private ArrayList<Resolution> cameraResolutions;
    private int selectedSizeWidth;
    private int selectedSizeHeight;

    public void setCameraResolutions(ArrayList<Resolution> cameraResolutions, Resolution selectedSize) {
        this.cameraResolutions = cameraResolutions;
        this.selectedSizeWidth = selectedSize.width;
        this.selectedSizeHeight = selectedSize.height;
        resolutionAdapter.setCameResolutions(cameraResolutions);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putSerializable(CAMERA_RESOLUTIONS, cameraResolutions);
        outState.putInt(SELECTED_SIZE_WIDTH, selectedSizeWidth);
        outState.putInt(SELECTED_SIZE_HEIGHT, selectedSizeHeight);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        restoreState(savedInstanceState);
        View v = inflater.inflate(R.layout.fragment_camera_resolutions, container, false);
        cameraResolutionsListView = (ListView) v.findViewById(R.id.camera_resolutions_listview);
        cameraResolutionsListView.setAdapter(resolutionAdapter);
        cameraResolutionsListView.setOnItemClickListener(this);
        cameraResolutionsListView.setChoiceMode(ListView.CHOICE_MODE_SINGLE);
        dialog = getDialog();
        return v;
    }

    @Override
    public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
        Resolution size = resolutionAdapter.getItem(i);
        setCameraResolution(size);
    }

    private void restoreState(Bundle savedInstanceState) {
        if (savedInstanceState != null) {
            if (savedInstanceState.containsKey(CAMERA_RESOLUTIONS)) {
                this.cameraResolutions = (ArrayList<Resolution>) savedInstanceState.getSerializable(CAMERA_RESOLUTIONS);
            }

            if (savedInstanceState.containsKey(SELECTED_SIZE_WIDTH) && savedInstanceState.containsKey(SELECTED_SIZE_WIDTH)) {
                selectedSizeWidth = savedInstanceState.getInt(SELECTED_SIZE_WIDTH);
                selectedSizeHeight = savedInstanceState.getInt(SELECTED_SIZE_HEIGHT);
            }

            resolutionAdapter.setCameResolutions(cameraResolutions);
        }
    }

    private void setCameraResolution(Resolution size) {
        if (getActivity() instanceof QiscusStreamActivity) {
            ((QiscusStreamActivity)getActivity()).setResolution(size);
        }
    }

    class CameResolutionsAdapter extends BaseAdapter {
        ArrayList<Resolution> cameraResolutions;

        public void setCameResolutions(ArrayList<Resolution> cameraResolutions) {
            this.cameraResolutions = cameraResolutions;
        }

        @Override
        public int getCount() {
            return cameraResolutions.size();
        }

        @Override
        public Resolution getItem(int i) {
            return cameraResolutions.get(getCount()-1-i);
        }

        @Override
        public long getItemId(int i) {
            return i;
        }

        @Override
        public View getView(int i, View convertView, ViewGroup viewGroup) {
            ViewHolder holder;

            if (convertView == null) {
                convertView = LayoutInflater.from(getActivity()).inflate(android.R.layout.simple_list_item_single_choice, null);
                holder = new ViewHolder();
                holder.resolutionText = (TextView) convertView.findViewById(android.R.id.text1);
                convertView.setTag(holder);
            } else {
                holder = (ViewHolder) convertView.getTag();
            }

            Resolution size =  getItem(i);

            if (size.width == selectedSizeWidth && size.height == selectedSizeHeight) {
                {
                    cameraResolutionsListView.setItemChecked(i, true);
                }
            }

            String resolutionText = size.width + " x " + size.height;
            holder.resolutionText.setText(resolutionText);
            return convertView;
        }

        public class ViewHolder {
            public TextView resolutionText;
        }
    }
}