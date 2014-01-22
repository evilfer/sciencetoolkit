package org.greengin.sciencetoolkit.ui.base.widgets;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;

public class ButtonPanelLayout extends ViewGroup {

	public ButtonPanelLayout(Context context) {
		super(context);
		init();
	}

	public ButtonPanelLayout(Context context, AttributeSet attrs) {
		super(context, attrs);
		init();
	}

	public ButtonPanelLayout(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		init();
	}

	private void init() {

	}

	@Override
	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
		int width = MeasureSpec.getSize(widthMeasureSpec);
		int wspec = MeasureSpec.makeMeasureSpec(width / (getChildCount() + 1), MeasureSpec.AT_MOST);
		int hspec = MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED);

		int height = 0;
		for (int i = 0; i < getChildCount(); i++) {
			View v = getChildAt(i);
			v.measure(wspec, hspec);
			height = Math.max(height, v.getMeasuredHeight());
		}
		
		this.setMeasuredDimension(width, height);
	}

	@Override
	protected void onLayout(boolean changed, int l, int t, int r, int b) {
		int height = b - t;
		int width = r - l;
		int n = this.getChildCount();
		
		int maxWidth = 0;
		
		for (int i = 0; i < n; i++) {
			maxWidth = Math.max(maxWidth, getChildAt(i).getMeasuredWidth());
		}
		
		int gap = (width - maxWidth * n) / (n + 1); 
		int widthWithGap = maxWidth + gap;
		int halfWidthWithGap = gap + maxWidth / 2;

		for (int i = 0; i < this.getChildCount(); i++) {
			View v = getChildAt(i);
			int vwidth = v.getMeasuredWidth();
			int p = halfWidthWithGap + widthWithGap * i - vwidth/2;
			v.layout(p, 0, p + vwidth, height);
		}
	}

}