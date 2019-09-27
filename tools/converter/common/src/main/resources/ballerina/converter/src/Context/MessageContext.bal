import ballerina/http;
import ballerina/log;

public type MessageContext object {
    CONTENT_TYPE contentType = TEXT_PLAIN;
    any payload = "";
    map<any> variables = {};

    public function __init(http:Request request) {
        if (APPLICATION_JSON == request.getContentType()) {
            self.contentType = APPLICATION_JSON;
            json|error jsonPayload = request.getJsonPayload();
            if (jsonPayload is json) {
                self.payload = jsonPayload;
            } else {
                log:printError("Error occurred while retrieving request payload", jsonPayload);
            }
        } else if (APPLICATION_XML == request.getContentType()) {
            self.contentType = APPLICATION_XML;
        }
    }
    
    public function getContentType() returns string {
        return self.contentType;
    }

    public function addVariable(string name, any data) {
        self.variables[name] = data;
    }

    public function getVariable(string name) returns any {
        return self.variables[name];
    }

    public function getPayload() returns any {
        return self.payload;
    }

    public function setPayload(any payload) {
        self.payload = payload;
        // Update content type
        if (payload is json) {
            self.contentType = APPLICATION_JSON;
        } else if (payload is xml) {
            self.contentType = APPLICATION_XML;
        } else {
            self.contentType = TEXT_PLAIN;
        }
    }
};
