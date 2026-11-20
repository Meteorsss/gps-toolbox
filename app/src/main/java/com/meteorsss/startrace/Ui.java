package com.meteorsss.startrace;

import android.content.Context;
import android.graphics.*;
import android.graphics.drawable.*;
import android.view.*;
import android.widget.*;

public final class Ui {
  public final Context context;
  public final int bg, card, border, text, muted, accent, warning;
  public final boolean light;

  public Ui(Context c, boolean light) {
    context = c;
    this.light = light;
    bg = Color.parseColor(light ? "#F3F6FB" : "#0C1421");
    card = Color.parseColor(light ? "#FFFFFF" : "#152236");
    border = Color.parseColor(light ? "#DAE3EF" : "#2A3A51");
    text = Color.parseColor(light ? "#152B40" : "#EDF6FF");
    muted = Color.parseColor(light ? "#566E85" : "#9AADC4");
    accent = Color.parseColor(light ? "#007B77" : "#68DED3");
    warning = Color.parseColor(light ? "#9B5B0A" : "#F1BB6B");
  }

  public int dp(float n) {
    return Math.round(n * context.getResources().getDisplayMetrics().density);
  }

  public TextView text(String value, int size, int color) {
    TextView t = new TextView(context);
    t.setText(value);
    t.setTextSize(size);
    t.setTextColor(color);
    t.setFontFeatureSettings("tnum");
    t.setLineSpacing(dp(3), 1);
    t.setIncludeFontPadding(false);
    return t;
  }

  public TextView title(String value, int size) {
    TextView t = text(value, size, text);
    t.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
    return t;
  }

  public LinearLayout column() {
    LinearLayout v = new LinearLayout(context);
    v.setOrientation(LinearLayout.VERTICAL);
    return v;
  }

  public LinearLayout row() {
    LinearLayout v = new LinearLayout(context);
    v.setOrientation(LinearLayout.HORIZONTAL);
    v.setGravity(Gravity.CENTER_VERTICAL);
    return v;
  }

  public GradientDrawable shape(int color, int radius, boolean stroke) {
    GradientDrawable d = new GradientDrawable();
    d.setColor(color);
    d.setCornerRadius(dp(radius));
    if (stroke) d.setStroke(dp(1), border);
    return d;
  }

  public LinearLayout panel() {
    LinearLayout p = column();
    p.setPadding(dp(18), dp(18), dp(18), dp(18));
    p.setBackground(shape(card, 22, true));
    LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
    lp.bottomMargin = dp(12);
    p.setLayoutParams(lp);
    return p;
  }

  public void gap(LinearLayout p, int size) {
    View v = new View(context);
    p.addView(v, new LinearLayout.LayoutParams(1, dp(size)));
  }

  public TextView button(String label, boolean primary, Runnable action) {
    TextView b = title(label, 14);
    b.setGravity(Gravity.CENTER);
    b.setMinHeight(dp(48));
    b.setPadding(dp(12), dp(12), dp(12), dp(12));
    b.setTextColor(primary ? (light ? Color.WHITE : bg) : text);
    StateListDrawable state = new StateListDrawable();
    state.addState(
        new int[] {android.R.attr.state_pressed}, shape(primary ? muted : border, 15, false));
    state.addState(new int[] {}, shape(primary ? accent : card, 15, !primary));
    b.setBackground(state);
    b.setOnClickListener(v -> action.run());
    b.setFocusable(true);
    b.setContentDescription(label);
    return b;
  }

  public void pair(LinearLayout parent, View a, View b) {
    LinearLayout row = row();
    LinearLayout.LayoutParams left = new LinearLayout.LayoutParams(0, -2, 1);
    left.rightMargin = dp(6);
    LinearLayout.LayoutParams right = new LinearLayout.LayoutParams(0, -2, 1);
    right.leftMargin = dp(6);
    row.addView(a, left);
    row.addView(b, right);
    parent.addView(row);
  }

  public void section(LinearLayout parent, String eyebrow, String title) {
    TextView e = text(eyebrow, 11, accent);
    e.setLetterSpacing(.12f);
    parent.addView(e);
    gap(parent, 9);
    parent.addView(title(title, 25));
    gap(parent, 18);
  }

  public TextView note(LinearLayout p, String text) {
    TextView t = text(text, 12, muted);
    p.addView(t);
    return t;
  }

  public int constellation(int system) {
    int[] colors = {
      muted,
      Color.rgb(104, 222, 211),
      Color.rgb(197, 167, 255),
      Color.rgb(255, 179, 128),
      Color.rgb(240, 209, 113),
      Color.rgb(135, 177, 255),
      Color.rgb(242, 137, 157),
      Color.rgb(175, 214, 144)
    };
    return colors[Math.max(0, Math.min(system, colors.length - 1))];
  }
}
