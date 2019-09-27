import bi/Context;
import ballerina/http;
import ballerina/log;

public function respond (Context:MessageContext context, http:Caller caller) returns error? {
    log:printInfo("Respond HIT");
    if (context.getContentType() == Context:APPLICATION_JSON) {
        log:printInfo("Responding JSON payload");
        var message = <json>context.getPayload();
        var result = caller->respond(message);
    } else if (context.getContentType() == Context:APPLICATION_XML) {
        log:printInfo("Responding XML payload");
        var message = <xml>context.getPayload();
        var result = caller->respond(message);
    } else {
        log:printInfo("Responding TEXT payload");
        var message = <string>context.getPayload();
        var result = caller->respond(message);
    }
}