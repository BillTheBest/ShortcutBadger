package me.leolin.shortcutbadger;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.util.Log;

import android.os.Build;
import me.leolin.shortcutbadger.impl.*;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.LinkedList;
import java.util.List;


/**
 * @author Leo Lin
 */
public abstract class ShortcutBadger {

    private static final List<Class<? extends ShortcutBadger>> BADGERS = new LinkedList<Class<? extends ShortcutBadger>>();

    static {
        BADGERS.add(AdwHomeBadger.class);
//        BADGERS.add(AndroidHomeBadger.class);
//        BADGERS.add(Android2HomeBadger.class);
        BADGERS.add(ApexHomeBadger.class);
        BADGERS.add(LGHomeBadger.class);
        BADGERS.add(NewHtcHomeBadger.class);
        BADGERS.add(NovaHomeBadger.class);
        BADGERS.add(SamsungHomeBadger.class);
        BADGERS.add(SolidHomeBadger.class);
        BADGERS.add(SonyHomeBadger.class);
        BADGERS.add(XiaomiHomeBadger.class);
        BADGERS.add(AsusHomeLauncher.class);
    }

//    private static final String MESSAGE_NOT_SUPPORT_BADGE_COUNT = "ShortBadger is currently not support the badgeCount \"%d\"";
//    private static final String MESSAGE_NOT_SUPPORT_THIS_HOME = "ShortcutBadger is currently not support the home launcher package \"%s\"";

    private static final int MIN_BADGE_COUNT = 0;
    private static final int MAX_BADGE_COUNT = 99;

    private static ShortcutBadger mShortcutBadger;

    private ShortcutBadger() {
    }

    protected Context mContext;

    protected ShortcutBadger(Context context) {
        this.mContext = context;
    }

    protected abstract void executeBadge(int badgeCount) throws ShortcutBadgeException;

    public static void setBadge(Context context, int badgeCount) {
        badgeCount = Math.min(Math.max(MIN_BADGE_COUNT, badgeCount), MAX_BADGE_COUNT);

        //find the home launcher Package
        Intent intent = new Intent(Intent.ACTION_MAIN);
        intent.addCategory(Intent.CATEGORY_HOME);
        ResolveInfo resolveInfo = context.getPackageManager().resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY);
        String currentHomePackage = resolveInfo.activityInfo.packageName;

        try {
            ShortcutBadger shortcutBadger = getShortcutBadger(currentHomePackage, context);

//            //not support this home launcher package
//            if (shortcutBadger == null) {
//                String exceptionMessage = String.format(MESSAGE_NOT_SUPPORT_THIS_HOME, currentHomePackage);
//                throw new ShortcutBadgeException(exceptionMessage);
//            }

            if (shortcutBadger != null) shortcutBadger.executeBadge(badgeCount);
        } catch (Throwable e) {
            Log.w("ShortcutBadger", e);
//            throw new ShortcutBadgeException("Unable to execute badge:" + e.getMessage());
        }

    }

    private static ShortcutBadger getShortcutBadger(String currentHomePackage, Context context) throws IllegalAccessException, InstantiationException, NoSuchMethodException, InvocationTargetException {
        if (mShortcutBadger != null) {
            return mShortcutBadger;
        }

        // Workaround for Meizu:
        // Meizu declare 'com.android.launcher', but hold something else
        // Icons get duplicated on restart after badge change
        if (Build.MANUFACTURER.toLowerCase().contains("meizu")) {
            return null;
        }

        if (Build.MANUFACTURER.equalsIgnoreCase("Xiaomi")) {
            mShortcutBadger = new XiaomiHomeBadger(context);
            return mShortcutBadger;
        }

        for (Class<? extends ShortcutBadger> badger : BADGERS) {
            Constructor<? extends ShortcutBadger> constructor = badger.getConstructor(Context.class);
            ShortcutBadger shortcutBadger = constructor.newInstance(context);
            if (shortcutBadger.getSupportLaunchers().contains(currentHomePackage)) {
                mShortcutBadger = shortcutBadger;
                break;
            }
        }


        return mShortcutBadger;
    }

    public abstract List<String> getSupportLaunchers();

    protected String getEntryActivityName() {
        ComponentName componentName = mContext.getPackageManager().getLaunchIntentForPackage(mContext.getPackageName()).getComponent();
        return componentName.getClassName();
    }

    protected String getContextPackageName() {
        return mContext.getPackageName();
    }
}
