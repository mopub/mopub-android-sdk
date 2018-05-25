package com.mopub.mobileads;

import android.content.Intent;

import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;

/**
 * Matcher to compare Intents by components of the Intent.
 */
public class IntentIsEqual extends BaseMatcher<Intent> {

    private Intent intent;

    public IntentIsEqual(final Intent intent) {
        this.intent = intent;
    }

    @Override
    public boolean matches(final Object item) {
        if (!(item instanceof Intent)) {
            return false;
        }
        Intent otherIntent = (Intent) item;
        if (intent.getExtras() == null && otherIntent.getExtras() != null) {
            return false;
        } else if (intent.getExtras() != null && otherIntent.getExtras() == null) {
            return false;
        } else if (intent.getExtras() != null && otherIntent.getExtras() != null) {
            if (intent.getExtras().size() != otherIntent.getExtras().size()) {
                return false;
            }
            for(String key : intent.getExtras().keySet()) {
                if (!intent.getExtras().get(key).equals(otherIntent.getExtras().get(key))) {
                    return false;
                }
            }
        }

        if (intent.getAction() != null && !intent.getAction().equals(otherIntent.getAction())) {
            return false;
        } else if (intent.getAction() == null && otherIntent.getAction() != null) {
            return false;
        }

        if (intent.getComponent() != null && !intent.getComponent().equals(otherIntent.getComponent())) {
            return false;
        } else if (intent.getComponent() == null && otherIntent.getComponent() != null) {
            return false;
        }

        if (intent.getData() != null && !intent.getData().equals(otherIntent.getData())) {
            return false;
        } else if (intent.getData() == null && otherIntent.getData() != null) {
            return false;
        }

        return true;
    }

    @Override
    public void describeTo(final Description description) {
        description.appendText(intent.toString() + " extras: " + intent.getExtras());
    }
}
