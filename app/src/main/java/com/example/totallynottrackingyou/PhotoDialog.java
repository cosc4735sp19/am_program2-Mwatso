package com.example.totallynottrackingyou;


import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;

import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.NoSuchElementException;


/**
 * A simple {@link Fragment} subclass.
 */
public class PhotoDialog extends DialogFragment
{
    /* log tag */
    private static final String TAG = "PhotoDialog";

    /* widgets */
    private ImageView _photo_imageview;
    private FloatingActionButton _close_fab;

    public PhotoDialog()
    {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
    {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_photo_dialog, container, false);

        _photo_imageview = view.findViewById(R.id.photo_imageview);
        _close_fab = view.findViewById(R.id.close_fab);

        byte[] byteArray = getArguments().getByteArray("photo");
        Bitmap bitmap = BitmapFactory.decodeByteArray(byteArray, 0, byteArray.length);

        int width = bitmap.getWidth();
        int height = bitmap.getHeight();
        Log.w(TAG, "Width: " + width + " Height: " + height);

        int bounding = dpToPx(1000);

        float xScale = ((float) bounding) / width;
        float yScale = ((float) bounding) / height;
        float scale = (xScale <= yScale) ? xScale : yScale;

        Matrix matrix = new Matrix();
        matrix.postScale(scale, scale);
        Bitmap scaledBitmap = Bitmap.createBitmap(bitmap, 0, 0, width, height, matrix, true);
        BitmapDrawable result = new BitmapDrawable(scaledBitmap);

        _photo_imageview.setImageDrawable(result);

        _close_fab.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                dismiss();
            }
        });

        return view;
    }

    private int dpToPx(int dp) {
        float density = getActivity().getResources().getDisplayMetrics().density;
        return Math.round((float)dp * density);
    }

}
