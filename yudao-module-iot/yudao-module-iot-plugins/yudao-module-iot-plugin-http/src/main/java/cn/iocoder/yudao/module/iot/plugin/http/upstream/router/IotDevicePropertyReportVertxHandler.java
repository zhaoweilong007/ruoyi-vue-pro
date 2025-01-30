package cn.iocoder.yudao.module.iot.plugin.http.upstream.router;

import cn.hutool.core.util.IdUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import cn.iocoder.yudao.module.iot.api.device.IotDeviceUpstreamApi;
import cn.iocoder.yudao.module.iot.api.device.dto.control.upstream.IotDevicePropertyReportReqDTO;
import cn.iocoder.yudao.module.iot.api.device.dto.control.upstream.IotDeviceStateUpdateReqDTO;
import cn.iocoder.yudao.module.iot.enums.device.IotDeviceStateEnum;
import cn.iocoder.yudao.module.iot.plugin.common.util.IotPluginCommonUtils;
import io.vertx.core.Handler;
import io.vertx.ext.web.RequestBody;
import io.vertx.ext.web.RoutingContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * IoT 设备属性上报的 Vert.x Handler
 */
@RequiredArgsConstructor
@Slf4j
public class IotDevicePropertyReportVertxHandler implements Handler<RoutingContext> {

    public static final String PATH = "/sys/:productKey/:deviceName/thing/event/property/post";

    private final IotDeviceUpstreamApi deviceUpstreamApi;

    @Override
    public void handle(RoutingContext ctx) {
        String productKey = ctx.pathParam("productKey");
        String deviceName = ctx.pathParam("deviceName");

        // TODO @haohao：requestBody.asJsonObject() 貌似天然就是 json 对象哈？
        RequestBody requestBody = ctx.body();
        JSONObject jsonData;
        try {
            jsonData = JSONUtil.parseObj(requestBody.asJsonObject());
        } catch (Exception e) {
            log.error("[HttpVertxHandler] 请求数据解析失败", e);
            ctx.response().setStatusCode(400)
                    .putHeader("Content-Type", "application/json; charset=UTF-8")
                    .end(createResponseJson(400, null, null,
                            "请求数据不是合法的 JSON 格式: " + e.getMessage(),
                            "thing.event.property.post", "1.0").toString());
            return;
        }

        String id = jsonData.getStr("id");

        try {
            // 设备上线
            deviceUpstreamApi.updateDeviceState(((IotDeviceStateUpdateReqDTO)
                    new IotDeviceStateUpdateReqDTO().setRequestId(IdUtil.fastSimpleUUID())
                            .setProcessId(IotPluginCommonUtils.getProcessId()).setReportTime(LocalDateTime.now())
                            .setProductKey(productKey).setDeviceName(deviceName))
                    .setState(IotDeviceStateEnum.ONLINE.getState()));

            // 属性上报
            deviceUpstreamApi.reportDeviceProperty(((IotDevicePropertyReportReqDTO)
                    new IotDevicePropertyReportReqDTO().setRequestId(IdUtil.fastSimpleUUID())
                            .setProcessId(IotPluginCommonUtils.getProcessId()).setReportTime(LocalDateTime.now())
                            .setProductKey(productKey).setDeviceName(deviceName))
                    .setProperties((Map<String, Object>) requestBody.asJsonObject().getMap().get("properties")));

            ctx.response()
                    .setStatusCode(200)
                    .putHeader("Content-Type", "application/json; charset=UTF-8")
                    .end(createResponseJson(200, new JSONObject(), id, "success",
                            "thing.event.property.post", "1.0").toString());
        } catch (Exception e) {
            log.error("[HttpVertxHandler] 上报属性数据失败", e);
            ctx.response()
                    .setStatusCode(500)
                    .putHeader("Content-Type", "application/json; charset=UTF-8")
                    .end(createResponseJson(500, new JSONObject(), id,
                            "The format of result is error!",
                            "thing.event.property.post", "1.0").toString());
        }
    }

    // TODO @芋艿：抽一个 IotPluginCommonResult 出来？等 mqtt、websocket 出来后，再考虑优化！
    private JSONObject createResponseJson(int code, JSONObject data, String id,
                                          String message, String method, String version) {
        JSONObject res = new JSONObject();
        res.set("code", code);
        res.set("data", data != null ? data : new JSONObject());
        res.set("id", id);
        res.set("message", message);
        res.set("method", method);
        res.set("version", version);
        return res;
    }

}
