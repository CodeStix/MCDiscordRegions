import axios from "axios";
import { debug } from "debug";

const logger = debug("mcdr:minecraft");

export async function getIGN(uuid: string): Promise<string | null> {
    try {
        let resp = await axios.get(
            `https://sessionserver.mojang.com/session/minecraft/profile/${encodeURIComponent(uuid)}`,
            {
                responseType: "json",
            }
        );
        return resp.data.name;
    } catch (ex) {
        logger("could not get minecraft ign for %s: %o", uuid, ex);
        return null;
    }
}
