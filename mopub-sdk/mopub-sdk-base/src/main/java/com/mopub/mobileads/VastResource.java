package com.mopub.mobileads;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.mopub.common.Preconditions;

import java.io.Serializable;
import java.util.Arrays;
import java.util.List;

/**
 * The data and its type used to populate a Vast 3.0 companion ad or icon.
 */
class VastResource implements Serializable {
    private static final long serialVersionUID = 0L;

    private static final List<String> VALID_IMAGE_TYPES =
            Arrays.asList("image/jpeg", "image/png", "image/bmp", "image/gif");
    private static final List<String> VALID_APPLICATION_TYPES =
            Arrays.asList("application/x-javascript");

    /**
     * The type of resource ordered according to priority.
     */
    enum Type {
        STATIC_RESOURCE,
        HTML_RESOURCE,
        IFRAME_RESOURCE
    }

    /**
     * The type of the static resource. Only static resources only will have values other than NONE.
     */
    enum CreativeType {
        NONE,
        IMAGE,
        JAVASCRIPT
    }

    @NonNull private String mResource;
    @NonNull private Type mType;
    @NonNull private CreativeType mCreativeType;
    private int mWidth;
    private int mHeight;

    /**
     * Helper method that tries to create a {@link VastResource} by accessing all resource types on
     * the {@link VastResourceXmlManager} in order of priority defined by the {@link Type} enum.
     *
     * @param resourceXmlManager the manager used to populate the {@link VastResource}
     * @param width              the expected width of the resource. This only affects IFrames.
     * @param height             the expected height of the resource. This only affects IFrames.
     * @return the newly created VastResource
     */
    @Nullable
    static VastResource fromVastResourceXmlManager(
            @NonNull final VastResourceXmlManager resourceXmlManager, final int width,
            final int height) {
        for (Type type : Type.values()) {
            VastResource vastResource =
                    fromVastResourceXmlManager(resourceXmlManager, type, width, height);
            if (vastResource != null) {
                return vastResource;
            }
        }
        return null;
    }

    /**
     * Tries to create a {@link VastResource} by accessing a specific resource {@link Type} on the
     * {@link VastResourceXmlManager}.
     *
     * @param resourceXmlManager the manager used to populate the {@link VastResource}
     * @param type the resource {@link Type} to try to access
     * @param width the expected width of the resource. This only affects IFrames.
     * @param height the expected height of the resource. This only affects IFrames.
     * @return the newly created VastResource
     */
    @Nullable
    static VastResource fromVastResourceXmlManager(
            @NonNull final VastResourceXmlManager resourceXmlManager,
            final @NonNull Type type, final int width, final int height) {
        Preconditions.checkNotNull(resourceXmlManager);
        Preconditions.checkNotNull(type);

        String iFrameResource = resourceXmlManager.getIFrameResource();
        String htmlResource = resourceXmlManager.getHTMLResource();
        String staticResource = resourceXmlManager.getStaticResource();
        String staticResourceType = resourceXmlManager.getStaticResourceType();

        String resource;
        CreativeType creativeType;

        if (type == Type.STATIC_RESOURCE &&
                staticResource != null && staticResourceType != null
                && (VALID_IMAGE_TYPES.contains(staticResourceType)
                || VALID_APPLICATION_TYPES.contains(staticResourceType))) {
            resource = staticResource;
            if (VALID_IMAGE_TYPES.contains(staticResourceType)) {
                creativeType = CreativeType.IMAGE;
            } else {
                creativeType = CreativeType.JAVASCRIPT;
            }
        } else if (type == Type.HTML_RESOURCE && htmlResource != null) {
            resource = htmlResource;
            creativeType = CreativeType.NONE;
        } else if (type == Type.IFRAME_RESOURCE && iFrameResource != null) {
            resource = iFrameResource;
            creativeType = CreativeType.NONE;
        } else {
            return null;
        }

        return new VastResource(resource, type, creativeType, width, height);
    }

    /**
     * Private constructor. Use fromVastResourceXmlManager() to create a VastResource.
     */
    VastResource(@NonNull final String resource, @NonNull final Type type,
            @NonNull final CreativeType creativeType, final int width, final int height) {
        Preconditions.checkNotNull(resource);
        Preconditions.checkNotNull(type);
        Preconditions.checkNotNull(creativeType);

        mResource = resource;
        mType = type;
        mCreativeType = creativeType;
        mWidth = width;
        mHeight = height;
    }

    @NonNull
    public String getResource() {
        return mResource;
    }

    @NonNull
    public Type getType() {
        return mType;
    }

    @NonNull
    public CreativeType getCreativeType() {
        return mCreativeType;
    }

    /**
     * Initializes a WebView used to display the resource.
     *
     * @param webView the resource's WebView.
     */
    public void initializeWebView(@NonNull VastWebView webView) {
        Preconditions.checkNotNull(webView);

        if (mType == Type.IFRAME_RESOURCE) {
            webView.loadData("<iframe frameborder=\"0\" scrolling=\"no\" marginheight=\"0\" " +
                    "marginwidth=\"0\" style=\"border: 0px; margin: 0px;\" width=\"" + mWidth +
                    "\" height=\"" + mHeight + "\" src=\"" + mResource + "\"></iframe>");
        } else if (mType == Type.HTML_RESOURCE) {
            webView.loadData(mResource);
        } else if (mType == Type.STATIC_RESOURCE) {
            if (mCreativeType == CreativeType.IMAGE) {
                String data = "<html>" +
                        "<head>" +
                        "</head>" +
                        // Set margin and padding to 0 in order to get rid of Android WebView
                        // default padding
                        "<body style=\"margin:0;padding:0\">" +
                        "<img src=\"" + mResource + "\" width=\"100%\" style=\"max-width:100%;max-height:100%;\" />" +
                        "</body>" +
                        "</html>";
                webView.loadData(data);
            } else if (mCreativeType == CreativeType.JAVASCRIPT) {
                String data = "<script src=\"" + mResource + "\"></script>";
                webView.loadData(data);
            }
        }
    }

    /**
     * Selects the correct click through url based on the type of resource.
     *
     * @param vastClickThroughUrl    The click through url as specified in the vast document. This
     *                               is used with static images.
     * @param webViewClickThroughUrl The click through url when pertaining to Javascript, HTML,
     *                               IFrames that originated from a WebView.
     * @return String representing the correct click through for the resource type which may be
     * {@code null} if the correct click through url was not specified or {@code null}.
     */
    @Nullable
    public String getCorrectClickThroughUrl(@Nullable final String vastClickThroughUrl,
            @Nullable final String webViewClickThroughUrl) {
        switch (mType) {
            case STATIC_RESOURCE:
                if (VastResource.CreativeType.IMAGE == mCreativeType) {
                    return vastClickThroughUrl;
                } else if (VastResource.CreativeType.JAVASCRIPT == mCreativeType) {
                    return webViewClickThroughUrl;
                }
                return null;
            case HTML_RESOURCE:
            case IFRAME_RESOURCE:
                return webViewClickThroughUrl;
            default:
                return null;
        }
    }
}
