package com.codeark.notifier;

/**
 * An interface for receiving notification when GcmService.init() is done
 */
public interface OnInit {
    public void execute(String msg);
}
