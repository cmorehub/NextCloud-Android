/*
 *   ownCloud Android client application
 *
 *   @author masensio
 *   Copyright (C) 2015 ownCloud Inc.
 *
 *   This program is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License version 2,
 *   as published by the Free Software Foundation.
 *
 *   This program is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU General Public License for more details.
 *
 *   You should have received a copy of the GNU General Public License
 *   along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 */

package com.owncloud.android.ui.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.askey.qbee.atv.R;
import com.owncloud.android.lib.resources.shares.OCShare;
import com.owncloud.android.ui.TextDrawable;

import java.security.NoSuchAlgorithmException;
import java.util.List;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;

/**
 * Adapter to show a user/group in Share With List
 */
public class ShareUserListAdapter extends ArrayAdapter {

    private Context mContext;
    private List<OCShare> mShares;
    private ShareUserAdapterListener mListener;
    private float mAvatarRadiusDimension;

    public ShareUserListAdapter(Context context, int resource, List<OCShare> shares,
                                ShareUserAdapterListener listener) {
        super(context, resource);
        mContext= context;
        mShares = shares;
        mListener = listener;

        mAvatarRadiusDimension = context.getResources().getDimension(R.dimen.standard_padding);
    }

    @Override
    public int getCount() {
        return mShares.size();
    }

    @Override
    public Object getItem(int position) {
        return mShares.get(position);
    }

    @Override
    public long getItemId(int position) {
        return 0;
    }

    @NonNull
    @Override
    public View getView(final int position, View convertView, @NonNull ViewGroup parent) {
        View view = convertView;
        if (view == null) {
            LayoutInflater inflater = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            view = inflater.inflate(R.layout.share_user_item, parent, false);
        }

        if (mShares != null && mShares.size() > position) {
            OCShare share = mShares.get(position);

            TextView userName = view.findViewById(R.id.userOrGroupName);
            ImageView icon = view.findViewById(R.id.icon);
            final ImageView editShareButton = view.findViewById(R.id.editShareButton);
            final ImageView unshareButton = view.findViewById(R.id.unshareButton);

            String name = share.getSharedWithDisplayName();

            switch (share.getShareType()) {
                case GROUP:
                    name = getContext().getString(R.string.share_group_clarification, name);
                    setImage(icon, name, R.drawable.ic_group);
                    break;
                case EMAIL:
                    name = getContext().getString(R.string.share_email_clarification, name);
                    setImage(icon, name, R.drawable.ic_email);
                    break;
                case ROOM:
                    name = getContext().getString(R.string.share_room_clarification, name);
                    setImage(icon, name, R.drawable.ic_chat_bubble);
                    break;
                default:
                    setImage(icon, name, R.drawable.ic_user);
                    break;
            }

            userName.setText(name);

            /// bind listener to edit privileges
            editShareButton.setOnClickListener(v -> mListener.editShare(mShares.get(position)));

            /// bind listener to unshare
            unshareButton.setOnClickListener(v -> mListener.unshareButtonPressed(mShares.get(position)));
        }
        return view;
    }

    private void setImage(ImageView icon, String name, @DrawableRes int fallback) {
        try {
            icon.setImageDrawable(TextDrawable.createNamedAvatar(name, mAvatarRadiusDimension));
        } catch (NoSuchAlgorithmException e) {
            icon.setImageResource(fallback);
        }
    }

    public interface ShareUserAdapterListener {
        void unshareButtonPressed(OCShare share);
        void editShare(OCShare share);
    }
}
