package com.android.launcher3.iconpack;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.TextView;

import androidx.preference.Preference;

import com.android.launcher3.R;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CustomIconPreference extends Preference {

    private final PackageManager pm;

    public CustomIconPreference(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        setLayoutResource(R.layout.item_preferences);
        pm = context.getPackageManager();
        init();
    }

    public CustomIconPreference(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public CustomIconPreference(Context context) {
        this(context, null);
    }

    private void init() {
        String currentPack = getPersistedString("");
        if (currentPack.isEmpty()) {
            setNone();
        } else {
            try {
                ApplicationInfo info = pm.getApplicationInfo(currentPack, 0);
                setIcon(info.loadIcon(pm));
                setSummary(info.loadLabel(pm));
            } catch (PackageManager.NameNotFoundException e) {
                setNone();
                persistString("");
            }
        }
    }

    private void setNone() {
        setIcon(getContext().getResources().getDrawable(R.mipmap.ic_launcher_home));
        setSummary(getContext().getResources().getString(R.string.icon_pack_none));
    }

    @Override
    protected void onClick() {
        super.onClick();
        showDialog();
    }

    protected void showDialog() {
        final Map<String, IconPackInfo> packages = loadAvailableIconPacks();
        final IconAdapter adapter = new IconAdapter(getContext(), packages, getPersistedString(""));
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setAdapter(adapter, (dialog, position) -> {
            String item = adapter.getItem(position);
            persistString(item);
            if (!item.isEmpty()) {
                IconPackInfo packInfo = packages.get(item);
                if (packInfo != null) {
                    setIcon(packInfo.icon);
                    setSummary(packInfo.label);
                }
            } else {
                setNone();
            }
        });
        builder.show();
    }

    private Map<String, IconPackInfo> loadAvailableIconPacks() {
        Map<String, IconPackInfo> iconPacks = new HashMap<>();
        List<ResolveInfo> list;
        list = pm.queryIntentActivities(new Intent("com.novalauncher.THEME"), 0);
        list.addAll(pm.queryIntentActivities(new Intent("org.adw.launcher.icons.ACTION_PICK_ICON"), 0));
        list.addAll(pm.queryIntentActivities(new Intent("com.dlto.atom.launcher.THEME"), 0));
        list.addAll(pm.queryIntentActivities(new Intent("android.intent.action.MAIN").addCategory("com.anddoes.launcher.THEME"), 0));
        list.addAll(pm.queryIntentActivities(new Intent("net.oneplus.launcher.icons.ACTION_PICK_ICON"), 0));
        for (ResolveInfo info : list) {
            iconPacks.put(info.activityInfo.packageName, new IconPackInfo(info, pm));
        }
        return iconPacks;
    }

    private static class IconPackInfo {
        String packageName;
        CharSequence label;
        Drawable icon;

        IconPackInfo(ResolveInfo r, PackageManager packageManager) {
            packageName = r.activityInfo.packageName;
            icon = r.loadIcon(packageManager);
            label = r.loadLabel(packageManager);
        }

        public IconPackInfo(String label, Drawable icon, String packageName) {
            this.label = label;
            this.icon = icon;
            this.packageName = packageName;
        }
    }

    private static class IconAdapter extends BaseAdapter {
        ArrayList<IconPackInfo> mSupportedPackages;
        LayoutInflater mLayoutInflater;
        String mCurrentIconPack;

        IconAdapter(Context context, Map<String, IconPackInfo> supportedPackages, String currentPack) {
            mLayoutInflater = LayoutInflater.from(context);
            mSupportedPackages = new ArrayList<>(supportedPackages.values());
            Collections.sort(mSupportedPackages, new Comparator<IconPackInfo>() {
                @Override
                public int compare(IconPackInfo lhs, IconPackInfo rhs) {
                    return lhs.label.toString().compareToIgnoreCase(rhs.label.toString());
                }
            });

            Resources res = context.getResources();
            String defaultLabel = context.getResources().getString(R.string.icon_pack_none);
            Drawable icon = res.getDrawable(R.mipmap.ic_launcher_home);
            mSupportedPackages.add(0, new IconPackInfo(defaultLabel, icon, ""));
            mCurrentIconPack = currentPack;
        }

        @Override
        public int getCount() {
            return mSupportedPackages.size();
        }

        @Override
        public String getItem(int position) {
            return mSupportedPackages.get(position).packageName;
        }

        @Override
        public long getItemId(int position) {
            return 0;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = mLayoutInflater.inflate(R.layout.iconpack_dialog, null);
            }
            IconPackInfo info = mSupportedPackages.get(position);
            TextView txtView = (TextView) convertView.findViewById(R.id.title);
            txtView.setText(info.label);
            ImageView imgView = (ImageView) convertView.findViewById(R.id.icon);
            imgView.setImageDrawable(info.icon);
            RadioButton radioButton = (RadioButton) convertView.findViewById(R.id.radio);
            radioButton.setChecked(info.packageName.equals(mCurrentIconPack));
            return convertView;
        }
    }
}