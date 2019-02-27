/* This is free and unencumbered software released into the public domain. */

package com.github.drydart.flutter_lua;

import io.flutter.plugin.common.MethodCall;
import io.flutter.plugin.common.MethodChannel;
import io.flutter.plugin.common.MethodChannel.Result;
import io.flutter.plugin.common.PluginRegistry.Registrar;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.ExecutorService;
import java.util.function.BiConsumer;
import java.util.function.Supplier;

/** FlutterLuaThreadHandler */
@SuppressWarnings("unchecked")
final class FlutterLuaThreadHandler extends FlutterMethodCallHandler {
  private static final String TAG = "FlutterLuaThreadHandler";
  public static final String CHANNEL_PREFIX = FlutterLuaPlugin.CHANNEL;

  private final MethodChannel channel;
  private final ExecutorService executor;
  private final State state;

  FlutterLuaThreadHandler(final Registrar registrar,
                          final long threadID) {
    super(registrar);
    final String channelName = String.format("%s/#%d", CHANNEL_PREFIX, threadID);
    this.channel = new MethodChannel(registrar.messenger(), channelName);
    this.executor = Executors.newSingleThreadScheduledExecutor();
    this.state = Flutter_lua.newState();
    channel.setMethodCallHandler(this);
  }

  @Override
  public void onMethodCall(final MethodCall call,
                           final Result result) {
    assert(call != null);
    assert(result != null);

    assert(call.method != null);
    switch (call.method) {

      case "evalString": {
        final State state = this.state;
        final String code = (String)call.arguments;
        CompletableFuture
          .supplyAsync(new Supplier() {
            @Override
            public Object get() {
              try {
                state.doString(code);
                return popResult(state);
              }
              catch (final Exception error) {
                throw new RuntimeException(error);
              }
            }
          }, this.executor)
          .whenComplete(new BiConsumer<Object, Throwable>() {
            @Override
            public void accept(final Object value, final Throwable error) {
              if (error != null) {
                result.error("Exception", error.getMessage(), error.toString());
              }
              else {
                result.success(value);
              }
            }
          });
        break;
      }

      case "evalFile": {
        result.notImplemented(); // TODO
      }

      case "evalAsset": {
        result.notImplemented(); // TODO
      }

      default: {
        result.notImplemented();
      }
    }
  }

  private static Object popResult(final State state) {
    if (!state.hasResult()) return null;
    switch ((int)state.resultType()) {
      case 0:  // lua.TypeNil
        return null;
      case 1:  // lua.TypeBoolean
        return state.boolValue();
      case 2:  // lua.TypeLightUserData
        return null; // TODO
      case 3:  // lua.TypeNumber
        return state.doubleValue(); // TODO: support integers
      case 4:  // lua.TypeString
        return state.stringValue();
      case 5:  // lua.TypeTable
        return null; // TODO
      case 6:  // lua.TypeFunction
        return null; // TODO
      case 7:  // lua.TypeUserData
        return null; // TODO
      case 8:  // lua.TypeThread
        return null; // TODO
      case 9:  // lua.TypeCount
      case -1: // lua.TypeNone
      default:
        assert(false); // unreachable
        return null; // unreachable
    }
  }
}
