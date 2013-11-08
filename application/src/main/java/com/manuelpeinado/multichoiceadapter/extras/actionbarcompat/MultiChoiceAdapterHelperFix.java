package com.manuelpeinado.multichoiceadapter.extras.actionbarcompat;

import java.lang.reflect.Method;

import android.app.Activity;
import android.support.v7.view.ActionMode;
import android.widget.BaseAdapter;

import com.manuelpeinado.multichoiceadapter.MultiChoiceAdapterHelperBase;

public class MultiChoiceAdapterHelperFix extends MultiChoiceAdapterHelperBase {
    private ActionMode actionMode;

    protected MultiChoiceAdapterHelperFix(BaseAdapter owner) {
        super(owner);
    }

    @Override
    protected void startActionMode() {
        try {
            Activity activity = (Activity) adapterView.getContext();
            Method method = activity.getClass().getMethod("startSupportActionMode", ActionMode.Callback.class);
            actionMode = (ActionMode) method.invoke(activity, owner);
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    @Override
    protected void finishActionMode() {
        if (actionMode != null) {
            actionMode.finish();
        }
    }

    @Override
    protected void setActionModeTitle(String title) {
        actionMode.setTitle(title);
    }

    @Override
    protected boolean isActionModeStarted() {
        return actionMode != null;
    }

    @Override
    protected void clearActionMode() {
        actionMode = null;
    }
}