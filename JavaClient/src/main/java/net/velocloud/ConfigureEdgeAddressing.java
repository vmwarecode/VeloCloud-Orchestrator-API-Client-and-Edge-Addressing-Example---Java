package net.velocloud;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.ParseException;

public class ConfigureEdgeAddressing {
    static PortalVCOAPIClient vcoClient = new PortalVCOAPIClient();
    public static void main(String args[]) throws ParseException {
        try {
            vcoClient.authenticate(System.getenv("VC_USERNAME"), System.getenv("VC_PASSWORD"), true);
            JSONObject obj = new JSONObject();
            obj.put("enterpriseId", Integer.parseInt(System.getenv("VC_ENTERPRISE_ID")));
            obj.put("edgeId", Integer.parseInt(System.getenv("VC_EDGE_ID")));
            System.out.println("<---------------------------------");
            System.out.println("Fetching edge specific configuration for:");
            System.out.println("EnterpriseId: " + Integer.parseInt(System.getenv("VC_ENTERPRISE_ID")));
            System.out.println("EdgeId: " +  Integer.parseInt(System.getenv("VC_EDGE_ID")));
            System.out.println("--------------------------------->\n");
            JSONArray profiles = vcoClient.callApi("edge/getEdgeConfigurationStack", obj, new JSONArray());
            JSONObject edgeSpecificProfile = (JSONObject) profiles.get(0);
            JSONArray modules = (JSONArray) edgeSpecificProfile.get("modules");
            JSONObject deviceSettingsModule = (JSONObject) getModuleByName(modules, "deviceSettings");
            JSONObject data = (JSONObject) deviceSettingsModule.get("data");
            JSONArray routedInterfaces = (JSONArray) data.get("routedInterfaces");
            String routedInterfaceName = "GE8";
            JSONObject routedInterface = (JSONObject) getRoutedInterfaceByName(routedInterfaces, routedInterfaceName);
            JSONObject l2 = new JSONObject();
            l2.put("autonegotiation", Boolean.TRUE);
            l2.put("MTU", 1500);
            l2.put("duplex", "FULL");
            l2.put("speed", "100M");
            routedInterface.put("l2", l2);
            updateConfigurationModule(Integer.parseInt(System.getenv("VC_ENTERPRISE_ID")), deviceSettingsModule);
        } catch(Exception e) {
            e.printStackTrace();
        }

    }
    private static JSONObject getModuleByName(JSONArray modules, String moduleName) {
        for (int i=0; i<modules.size(); i++) {
            JSONObject module = (JSONObject) modules.get(i);
            if (module.get("name").equals(moduleName)) {
                return module;
            }
        }
        return new JSONObject();
    }
    private static JSONObject getRoutedInterfaceByName(JSONArray routedInterfaces, String routedInterfaceName) {
        for (int i = 0; i<routedInterfaces.size(); i++) {
            JSONObject routedInterface = (JSONObject) routedInterfaces.get(i);
            if (routedInterface.get("name").equals(routedInterfaceName)) {
                return routedInterface;
            }
            
        }
        return new JSONObject();
    }
    private static void updateConfigurationModule(int enterpriseId, JSONObject module) {
        JSONObject updatedDeviceSettings = new JSONObject();
        updatedDeviceSettings.put("id", module.get("id"));
        updatedDeviceSettings.put("enterpriseId", enterpriseId);
        JSONObject updateObject = new JSONObject();
        updateObject.put("data", module.get("data"));
        updateObject.put("refs", module.get("refs"));
        updateObject.put("description", module.get("description"));
        updateObject.put("name", module.get("name"));
        updatedDeviceSettings.put("_update", updateObject);
        System.out.println("<---------------------------------");
        System.out.println("Updating edge specific configuration for:");
        System.out.println("EnterpriseId: " + Integer.parseInt(System.getenv("VC_ENTERPRISE_ID")));
        System.out.println("EdgeId: " +  Integer.parseInt(System.getenv("VC_EDGE_ID")));
        System.out.println("Update: " +  updateObject);
        System.out.println("--------------------------------->\n");
        vcoClient.callApi("configuration/updateConfigurationModule", updatedDeviceSettings, new JSONObject());
    }
}
