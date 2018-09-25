//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

package com.google.firebase.messaging;

import android.app.PendingIntent;
import android.app.PendingIntent.CanceledException;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.WorkerThread;
import android.text.TextUtils;
import android.util.Log;

import com.google.android.gms.common.internal.Hide;
import com.google.firebase.iid.zzb;
import com.google.firebase.iid.zzk;
import com.google.firebase.iid.zzz;

import java.util.ArrayDeque;
import java.util.Iterator;
import java.util.Queue;

public class FirebaseMessagingService1 extends zzb {
    private static final Queue<String> zzoma = new ArrayDeque(10);

    public FirebaseMessagingService1() {
    }

    @WorkerThread
    public void onMessageReceived(RemoteMessage var1) {
    }

    @WorkerThread
    public void onDeletedMessages() {
    }

    @WorkerThread
    public void onMessageSent(String var1) {
    }

    @WorkerThread
    public void onSendError(String var1, Exception var2) {
    }

    @Hide
    protected final Intent zzp(Intent var1) {
        return zzz.zzclq().zzclr();
    }

    public final boolean zzq(Intent var1) {
        if ("com.google.firebase.messaging.NOTIFICATION_OPEN".equals(var1.getAction())) {
            PendingIntent var4;
            if ((var4 = (PendingIntent) var1.getParcelableExtra("pending_intent")) != null) {
                try {
                    var4.send();
                } catch (CanceledException var5) {
                    Log.e("FirebaseMessaging", "Notification pending intent canceled");
                }
            }

            if (zzal(var1.getExtras())) {
                zzd.zzg(this, var1);
            }

            return true;
        } else {
            return false;
        }
    }

    @Hide
    public final void handleIntent(Intent var1) {
        String var2;
        if ((var2 = var1.getAction()) == null) {
            var2 = "";
        }

        byte var4 = -1;
        switch (var2.hashCode()) {
            case 75300319:
                if (var2.equals("com.google.firebase.messaging.NOTIFICATION_DISMISS")) {
                    var4 = 1;
                }
                break;
            case 366519424:
                if (var2.equals("com.google.android.c2dm.intent.RECEIVE")) {
                    var4 = 0;
                }
        }

        String var10001;
        String var10002;
        String var10003;
        switch (var4) {
            case 0:
                String var7;
                String var8;
                boolean var10000;
                if (TextUtils.isEmpty(var8 = var7 = var1.getStringExtra("google.message_id"))) {
                    var10000 = false;
                } else if (zzoma.contains(var8)) {
                    if (Log.isLoggable("FirebaseMessaging", 3)) {
                        var10002 = String.valueOf(var8);
                        if (var10002.length() != 0) {
                            var10001 = "Received duplicate message: ".concat(var10002);
                        } else {
                            var10003 = new String();
                            var10001 = var10003;
                            var10003 = ("Received duplicate message: ");
                        }

                        Log.d("FirebaseMessaging", var10001);
                    }

                    var10000 = true;
                } else {
                    if (zzoma.size() >= 10) {
                        zzoma.remove();
                    }

                    zzoma.add(var8);
                    var10000 = false;
                }

                if (!var10000) {
                    String var10;
                    if ((var10 = var1.getStringExtra("message_type")) == null) {
                        var10 = "gcm";
                    }

                    byte var12 = -1;
                    switch (var10.hashCode()) {
                        case -2062414158:
                            if (var10.equals("deleted_messages")) {
                                var12 = 1;
                            }
                            break;
                        case 102161:
                            if (var10.equals("gcm")) {
                                var12 = 0;
                            }
                            break;
                        case 814694033:
                            if (var10.equals("send_error")) {
                                var12 = 3;
                            }
                            break;
                        case 814800675:
                            if (var10.equals("send_event")) {
                                var12 = 2;
                            }
                    }

                    switch (var12) {
                        case 0:
                            if (zzal(var1.getExtras())) {
                                try {
                                    zzd.zzf(this, var1);
                                } catch (Exception e) {
                                    e.printStackTrace();
                                    return;
                                }

                            }

                            Bundle var17;
                            if ((var17 = var1.getExtras()) == null) {
                                var17 = new Bundle();
                            }

                            var17.remove("android.support.content.wakelockid");
                            if (zza.zzai(var17)) {
                              /*  if (zza.zzfc(this).zzt(var17)) {
                                    this.onMessageReceived(new RemoteMessage(var17));
                                    break;
                                }*/

                                try {
                                    if (zzal(var17)) {
                                        zzd.zzi(this, var1);
                                    }
                                } catch (Exception e) {
                                    e.printStackTrace();
                                    return;
                                }
                            }

                            this.onMessageReceived(new RemoteMessage(var17));
                            break;
                        case 1:
                            this.onDeletedMessages();
                            break;
                        case 2:
                            this.onMessageSent(var1.getStringExtra("google.message_id"));
                            break;
                        case 3:
                            String var15;
                            if ((var15 = var1.getStringExtra("google.message_id")) == null) {
                                var15 = var1.getStringExtra("message_id");
                            }

                            this.onSendError(var15, new SendException(var1.getStringExtra("error")));
                            break;
                        default:
                            var10002 = String.valueOf(var10);
                            if (var10002.length() != 0) {
                                var10001 = "Received message with unknown type: ".concat(var10002);
                            } else {
                                var10003 = new String();
                                var10001 = var10003;
                                var10003 = ("Received message with unknown type: ");
                            }

                            Log.w("FirebaseMessaging", var10001);
                    }
                }

                if (!TextUtils.isEmpty(var7)) {
                    Bundle var16;
                    (var16 = new Bundle()).putString("google.message_id", var7);
                    zzk.zzfa(this).zzm(2, var16);
                }

                return;
            case 1:
                if (zzal(var1.getExtras())) {
                    zzd.zzh(this, var1);
                    return;
                }
                break;
            default:
                var10002 = String.valueOf(var1.getAction());
                if (var10002.length() != 0) {
                    var10001 = "Unknown intent action: ".concat(var10002);
                } else {
                    var10003 = new String();
                    var10001 = var10003;
                    var10003 = ("Unknown intent action: ");
                }

                Log.d("FirebaseMessaging", var10001);
        }

    }

    static void zzr(Bundle var0) {
        Iterator var1 = var0.keySet().iterator();

        while (var1.hasNext()) {
            String var2;
            if ((var2 = (String) var1.next()) != null && var2.startsWith("google.c.")) {
                var1.remove();
            }
        }

    }

    static boolean zzal(Bundle var0) {
        return var0 == null ? false : "1".equals(var0.getString("google.c.a.e"));
    }
}
