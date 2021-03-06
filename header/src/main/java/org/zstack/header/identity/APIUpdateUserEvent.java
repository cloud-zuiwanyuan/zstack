package org.zstack.header.identity;

import org.zstack.header.message.APIEvent;

/**
 * Created by frank on 7/10/2015.
 */
public class APIUpdateUserEvent extends APIEvent {
    private UserInventory inventory;

    public UserInventory getInventory() {
        return inventory;
    }

    public void setInventory(UserInventory inventory) {
        this.inventory = inventory;
    }

    public APIUpdateUserEvent() {
    }

    public APIUpdateUserEvent(String apiId) {
        super(apiId);
    }
}
