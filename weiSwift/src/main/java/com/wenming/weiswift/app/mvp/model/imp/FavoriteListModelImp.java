package com.wenming.weiswift.app.mvp.model.imp;

import android.content.Context;

import com.sina.weibo.sdk.exception.WeiboException;
import com.sina.weibo.sdk.net.RequestListener;
import com.wenming.weiswift.app.api.FavoritesAPI;
import com.wenming.weiswift.app.common.NewFeature;
import com.wenming.weiswift.app.common.entity.Favorite;
import com.wenming.weiswift.app.common.entity.Status;
import com.wenming.weiswift.app.common.entity.list.FavoriteList;
import com.wenming.weiswift.app.common.oauth.AccessTokenManager;
import com.wenming.weiswift.app.common.oauth.constant.AppAuthConstants;
import com.wenming.weiswift.app.mvp.model.FavoriteListModel;
import com.wenming.weiswift.app.utils.TextSaveUtils;
import com.wenming.weiswift.utils.SDCardUtils;
import com.wenming.weiswift.utils.ToastUtil;
import com.wenming.weiswift.widget.toast.LoadedToast;

import java.util.ArrayList;

/**
 * Created by wenmingvs on 16/6/6.
 */
public class FavoriteListModelImp implements FavoriteListModel {
    private Context mContext;
    private OnRequestUIListener mOnRequestUIListener;
    private ArrayList<Status> mStatusList = new ArrayList<>();
    private int mPage = 1;

    @Override
    public void createFavorite(final Status status, Context context, OnRequestUIListener onRequestUIListener) {
        mContext = context;
        mOnRequestUIListener = onRequestUIListener;
        FavoritesAPI favoritesAPI = new FavoritesAPI(context, AppAuthConstants.APP_KEY, AccessTokenManager.getInstance().getOAuthToken());
        favoritesAPI.create(Long.valueOf(status.id), new RequestListener() {
            @Override
            public void onComplete(String s) {
                ToastUtil.showShort(mContext, "收藏成功");
                status.favorited = true;
                mOnRequestUIListener.onSuccess();
            }

            @Override
            public void onWeiboException(WeiboException e) {
                //如果是已经收藏的微博，需要纠正他
                if (e.getMessage().contains("20704")) {
                    ToastUtil.showShort(mContext, "请不要重复收藏");
                    status.favorited = true;
                } else {
                    ToastUtil.showShort(mContext, "收藏失败");
                    ToastUtil.showShort(mContext, e.getMessage());
                }
                mOnRequestUIListener.onError(e.getMessage());
            }
        });
    }

    @Override
    public void cancelFavorite(final Status status, Context context, OnRequestUIListener onRequestUIListener) {
        mContext = context;
        mOnRequestUIListener = onRequestUIListener;
        final FavoritesAPI favoritesAPI = new FavoritesAPI(context, AppAuthConstants.APP_KEY, AccessTokenManager.getInstance().getOAuthToken());
        favoritesAPI.destroy(Long.valueOf(status.id), new RequestListener() {
            @Override
            public void onComplete(String s) {
                ToastUtil.showShort(mContext, "取消收藏成功");
                status.favorited = false;
                mOnRequestUIListener.onSuccess();
            }

            @Override
            public void onWeiboException(WeiboException e) {
                ToastUtil.showShort(mContext, "取消收藏失败");
                ToastUtil.showShort(mContext, e.getMessage());
                mOnRequestUIListener.onError(e.getMessage());
            }
        });
    }

    @Override
    public void favorites(final Context context, final OnDataFinishedListener onDataFinishedListener) {
        mContext = context;
        FavoritesAPI favoritesAPI = new FavoritesAPI(context, AppAuthConstants.APP_KEY, AccessTokenManager.getInstance().getOAuthToken());
        favoritesAPI.favorites(NewFeature.GET_FAVORITE_NUMS, mPage, new RequestListener() {
            @Override
            public void onComplete(String response) {
                ArrayList<Favorite> temp = FavoriteList.parse(response).favorites;
                if (temp != null && temp.size() > 0) {
                    cacheSave(mContext, response);
                    for (Favorite favorite : temp) {
                        mStatusList.add(favorite.status);
                    }
                    mPage++;
                    onDataFinishedListener.onDataFinish(mStatusList);
                } else {
                    LoadedToast.showToast(context, "0条新微博");
                    onDataFinishedListener.noMoreDate();
                }
            }

            @Override
            public void onWeiboException(WeiboException e) {
                ToastUtil.showShort(context, e.getMessage());
                onDataFinishedListener.onError(e.getMessage());
                cacheLoad(context, onDataFinishedListener);
            }
        });
    }

    @Override
    public void favoritesNextPage(final Context context, final OnDataFinishedListener onDataFinishedListener) {
        mContext = context;
        FavoritesAPI favoritesAPI = new FavoritesAPI(context, AppAuthConstants.APP_KEY, AccessTokenManager.getInstance().getOAuthToken());
        favoritesAPI.favorites(NewFeature.GET_FAVORITE_NUMS, mPage, new RequestListener() {
            @Override
            public void onComplete(String response) {
                ArrayList<Favorite> temp = FavoriteList.parse(response).favorites;
                if (temp != null && temp.size() > 0) {
                    for (Favorite favorite : temp) {
                        mStatusList.add(favorite.status);
                    }
                    mPage++;
                    onDataFinishedListener.onDataFinish(mStatusList);
                } else {
                    ToastUtil.showShort(context, "没有更新的内容了");
                    onDataFinishedListener.noMoreDate();
                }
            }

            @Override
            public void onWeiboException(WeiboException e) {
                ToastUtil.showShort(context, e.getMessage());
                onDataFinishedListener.onError(e.getMessage());
            }
        });
    }

    @Override
    public void cacheSave(Context context, String response) {
        TextSaveUtils.write(SDCardUtils.getSdcardPath() + "/weiSwift/profile", "我的收藏" + AccessTokenManager.getInstance().getOAuthToken() + ".txt", response);
    }

    @Override
    public void cacheLoad(Context context, OnDataFinishedListener onDataFinishedListener) {
        String response = TextSaveUtils.read(SDCardUtils.getSdcardPath() + "/weiSwift/profile", "我的收藏" + AccessTokenManager.getInstance().getOAuthToken() + ".txt");
        if (response != null) {
            ArrayList<Favorite> temp = FavoriteList.parse(response).favorites;
            if (temp != null && temp.size() > 0) {
                mPage++;
                for (Favorite favorite : temp) {
                    mStatusList.add(favorite.status);
                }
                onDataFinishedListener.onDataFinish(mStatusList);
            }
        }
    }

}
