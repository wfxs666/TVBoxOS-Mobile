package com.github.tvbox.osc.ui.adapter;

import android.view.View;
import android.widget.TextView;

import com.chad.library.adapter.base.BaseQuickAdapter;
import com.chad.library.adapter.base.BaseViewHolder;
import com.github.tvbox.osc.R;
import com.github.tvbox.osc.bean.VodInfo;

import java.util.ArrayList;

/**
 * @author pj567
 * @date :2020/12/22
 * @description:
 */
public class SeriesFlagAdapter extends BaseQuickAdapter<VodInfo.VodSeriesFlag, BaseViewHolder> {
    public SeriesFlagAdapter() {
        super(R.layout.item_select_flag, new ArrayList<>());
    }

    @Override
    protected void convert(BaseViewHolder helper, VodInfo.VodSeriesFlag item) {
        View select = helper.getView(R.id.vFlag);
        if (item.selected) {
            select.setVisibility(View.VISIBLE);
            // 下划线跟随自定义主题色
            int tc = com.github.tvbox.osc.util.Utils.getThemeColor();
            if (tc != -1) {
                try {
                    select.setBackgroundColor(tc);
                } catch (Exception e) {
                }
            }
        } else {
            select.setVisibility(View.GONE);
        }
        helper.setText(R.id.tvFlag, item.name);
    }
}