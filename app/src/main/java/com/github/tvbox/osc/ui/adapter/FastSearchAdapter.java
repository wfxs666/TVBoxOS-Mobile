package com.github.tvbox.osc.ui.adapter;

import android.text.TextUtils;
import android.util.Log;
import android.widget.ImageView;
import android.widget.TextView;

import com.chad.library.adapter.base.BaseQuickAdapter;
import com.chad.library.adapter.base.BaseViewHolder;
import com.github.tvbox.osc.R;
import com.github.tvbox.osc.api.ApiConfig;
import com.github.tvbox.osc.bean.SourceBean;
import com.github.tvbox.osc.bean.Movie;
import com.github.tvbox.osc.picasso.RoundTransformation;
import com.github.tvbox.osc.util.ImgUtil;
import com.github.tvbox.osc.util.MD5;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;

import me.jessyan.autosize.utils.AutoSizeUtils;

public class FastSearchAdapter extends BaseQuickAdapter<Movie.Video, BaseViewHolder> {
    private static final String TAG = "FastSearchAdapter";

    public FastSearchAdapter() {
        super(R.layout.item_search, new ArrayList<>());
    }

    @Override
    protected void convert(BaseViewHolder helper, Movie.Video item) {
        helper.setText(R.id.tvName, item.name);
        SourceBean source = ApiConfig.get().getSource(item.sourceKey);
        helper.setText(R.id.tvSite, source == null ? "" : source.getName());
        // 右侧源名称: 自定义文字色 > 主题色 > 默认强调色; 标题tvName保持默认深色
        TextView tvSite = helper.getView(R.id.tvSite);
        try {
            int customText = com.github.tvbox.osc.util.Utils.getThemeTextColor();
            int tc = com.github.tvbox.osc.util.Utils.getThemeColor();
            if (customText != -1) {
                tvSite.setTextColor(customText);
            } else if (tc != -1) {
                tvSite.setTextColor(tc);
            } else {
                tvSite.setTextColor(mContext.getColor(R.color.colorPrimary));
            }
        } catch (Exception e) {
        }
        helper.setVisible(R.id.tvNote, item.note != null && !item.note.isEmpty());
        if (item.note != null && !item.note.isEmpty()) helper.setText(R.id.tvNote, item.note);
        ImageView ivThumb = helper.getView(R.id.ivThumb);
        if (!TextUtils.isEmpty(item.pic)) {
            Picasso.get()
                    .load(item.pic)
                    .transform(new RoundTransformation(MD5.string2MD5(item.pic + "position=" + helper.getLayoutPosition()))
                            .centerCorp(true)
                            .override(AutoSizeUtils.dp2px(mContext, 110), AutoSizeUtils.dp2px(mContext, 160))
                            .roundRadius(AutoSizeUtils.dp2px(mContext, 20), RoundTransformation.RoundType.ALL))
                    .placeholder(R.drawable.img_loading_placeholder)
                    .error(R.drawable.img_loading_placeholder)
                    .into(ivThumb);
        } else {
            Log.d(TAG, "empty image for item: " + item.name);
            ivThumb.setImageDrawable(ImgUtil.createTextDrawable(item.name));
        }
    }
}