package mindustry.input;

import arc.*;
import arc.Graphics.*;
import arc.Graphics.Cursor.*;
import arc.graphics.Color;
import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.input.*;
import arc.math.*;
import arc.math.geom.*;
import arc.scene.*;
import arc.scene.event.*;
import arc.scene.ui.*;
import arc.scene.ui.layout.*;
import arc.struct.*;
import arc.util.*;
import mindustry.*;
import mindustry.client.Client;
import mindustry.client.Spectate;
import mindustry.client.navigation.*;
import mindustry.client.navigation.waypoints.PayloadDropoffWaypoint;
import mindustry.client.navigation.waypoints.PositionWaypoint;
import mindustry.client.navigation.waypoints.Waypoint;
import mindustry.client.ui.UnitPicker;
import mindustry.core.*;
import mindustry.entities.units.*;
import mindustry.game.EventType.*;
import mindustry.game.*;
import mindustry.gen.*;
import mindustry.graphics.*;
import mindustry.type.UnitType;
import mindustry.ui.*;
import mindustry.ui.dialogs.BaseDialog;
import mindustry.world.*;
import mindustry.world.blocks.payloads.*;
import java.util.concurrent.atomic.*;
import static arc.Core.*;
import static mindustry.Vars.net;
import static mindustry.Vars.*;
import static mindustry.input.PlaceMode.*;

public class DesktopInput extends InputHandler{
    public Vec2 movement = new Vec2();
    /** Current cursor type. */
    public Cursor cursorType = SystemCursor.arrow;
    /** Position where the player started dragging a line. */
    public int selectX = -1, selectY = -1, schemX = -1, schemY = -1;
    /** Last known line positions.*/
    public int lastLineX, lastLineY, schematicX, schematicY;
    /** Whether selecting mode is active. */
    public PlaceMode mode;
    /** Animation scale for line. */
    public float selectScale;
    /** Selected build request for movement. */
    public @Nullable BuildPlan sreq;
    /** Whether player is currently deleting removal requests. */
    public boolean deleting = false, shouldShoot = false;
    public static boolean panning = false;
    /** Mouse pan speed. */
    public float panScale = 0.005f, panSpeed = 4.5f, panBoostSpeed = 11f;
    private long lastMineClicked = 0L;

    @Override
    public void buildUI(Group group){
        // Various hints
        group.fill(t -> {
            t.visible(() -> Core.settings.getBool("hints") && ui.hudfrag.shown && UnitType.alpha == 0);
            t.bottom();
            t.table(Styles.black6, b -> {
                b.defaults().left();
                b.label(() -> Core.bundle.format("toggleinvis", "SHIFT + " + Core.keybinds.get(Binding.invisible_units).key.toString()));
            }).margin(6f);
        });
        group.fill(t -> {
            t.visible(() -> Core.settings.getBool("hints") && ui.hudfrag.shown && Navigation.state == NavigationState.NONE && !player.dead() && !player.unit().spawnedByCore() && !player.unit().isBuilding() && !(Core.settings.getBool("hints") && lastSchematic != null && !selectRequests.isEmpty()) && UnitType.alpha != 0);
            t.bottom();
            t.table(Styles.black6, b -> {
                b.defaults().left();
                b.label(() -> Core.bundle.format("respawn", Core.keybinds.get(Binding.respawn).key.toString())).style(Styles.outlineLabel);
            }).margin(6f);
        });
        group.fill(t -> {
            t.visible(() -> Core.settings.getBool("hints") && ui.hudfrag.shown && Navigation.state == NavigationState.RECORDING && UnitType.alpha != 0);
            t.bottom();
            t.table(Styles.black6, b -> {
                b.defaults().left();
                b.label(() -> Core.bundle.format("waypoint", Core.keybinds.get(Binding.place_waypoint).key.toString())).style(Styles.outlineLabel);
            }).margin(6f);
        });
        group.fill(t -> {
            t.visible(() -> Core.settings.getBool("hints") && ui.hudfrag.shown && Navigation.state == NavigationState.FOLLOWING && UnitType.alpha != 0);
            t.bottom();
            t.table(Styles.black6, b -> {
                b.defaults().left();
                b.label(() -> Core.bundle.format("stoppath", Core.keybinds.get(Binding.stop_following_path).key.toString())).style(Styles.outlineLabel);
            }).margin(6f);
        });
        group.fill(t -> {
            t.visible(() -> Core.settings.getBool("hints") && ui.hudfrag.shown && Navigation.state == NavigationState.THINKING && UnitType.alpha != 0);
            t.bottom();
            t.table(Styles.black6, b -> {
                b.defaults().left();
                b.label(() -> Core.bundle.format("stopthinking", Core.keybinds.get(Binding.stop_following_path).key.toString())).style(Styles.outlineLabel);
            }).margin(6f);
        });

        group.fill(t -> {
            t.bottom();
            t.visible(() -> {
                t.color.a = Mathf.lerpDelta(t.color.a, player.unit().isBuilding() ? 1f : 0f, 0.15f);

                return ui.hudfrag.shown && Core.settings.getBool("hints") && selectRequests.isEmpty() && t.color.a > 0.01f && Navigation.state == NavigationState.NONE && UnitType.alpha != 0;
            });
            t.touchable(() -> t.color.a < 0.1f ? Touchable.disabled : Touchable.childrenOnly);
            t.table(Styles.black6, b -> {
                b.defaults().left();
                b.label(() -> Core.bundle.format(!isBuilding ?  "resumebuilding" : "pausebuilding", Core.keybinds.get(Binding.pause_building).key.toString())).style(Styles.outlineLabel);
                b.row();
                b.label(() -> Core.bundle.format("cancelbuilding", Core.keybinds.get(Binding.clear_building).key.toString())).style(Styles.outlineLabel);
                b.row();
                b.label(() -> Core.bundle.format("selectschematic", Core.keybinds.get(Binding.schematic_select).key.toString())).style(Styles.outlineLabel);
            }).margin(10f);
        });

        //schematic controls
        group.fill(t -> {
            t.visible(() -> ui.hudfrag.shown && lastSchematic != null && !selectRequests.isEmpty() && Navigation.state == NavigationState.NONE);
            t.bottom();
            t.table(Styles.black6, b -> {
                b.defaults().left();
                b.label(() -> Core.bundle.format("schematic.flip",
                    Core.keybinds.get(Binding.schematic_flip_x).key.toString(),
                    Core.keybinds.get(Binding.schematic_flip_y).key.toString())).style(Styles.outlineLabel).visible(() -> Core.settings.getBool("hints"));
                b.row();
                b.table(a -> {
                    a.button("@schematic.add", Icon.save, this::showSchematicSave).colspan(2).size(250f, 50f).disabled(f -> lastSchematic == null || lastSchematic.file != null);
                });
            }).margin(6f);
        });
    }

    @Override
    public void drawTop(){
        Lines.stroke(1f);
        int cursorX = tileX(Core.input.mouseX());
        int cursorY = tileY(Core.input.mouseY());

        //draw break selection
        if(mode == breaking){
            drawBreakSelection(selectX, selectY, cursorX, cursorY, /*!Core.input.keyDown(Binding.schematic_select) ? maxLength :*/ Vars.maxSchematicSize);
        }

        if(Core.input.keyDown(Binding.schematic_select) && !Core.scene.hasKeyboard() && mode != breaking){
            drawSelection(schemX, schemY, cursorX, cursorY, Vars.maxSchematicSize);
        }

        Draw.reset();
    }

    @Override
    public void drawBottom(){
        int cursorX = tileX(input.mouseX());
        int cursorY = tileY(input.mouseY());

        //draw request being moved
        if(sreq != null){
            boolean valid = validPlace(sreq.x, sreq.y, sreq.block, sreq.rotation, sreq);
            if(sreq.block.rotate){
                drawArrow(sreq.block, sreq.x, sreq.y, sreq.rotation, valid);
            }

            sreq.block.drawPlan(sreq, allRequests(), valid);

            drawSelected(sreq.x, sreq.y, sreq.block, getRequest(sreq.x, sreq.y, sreq.block.size, sreq) != null ? Pal.remove : Pal.accent);
        }

        //draw hover request
        if(mode == none && !isPlacing()){
            BuildPlan req = getRequest(cursorX, cursorY);
            if(req != null){
                drawSelected(req.x, req.y, req.breaking ? req.tile().block() : req.block, Pal.accent);
            }
        }

        //draw schematic requests
        selectRequests.each(req -> {
            req.animScale = 1f;
            drawRequest(req);
        });

        selectRequests.each(this::drawOverRequest);

        if(player.isBuilder()){
            //draw things that may be placed soon
            if(mode == placing && block != null){
                for(int i = 0; i < lineRequests.size; i++){
                    BuildPlan req = lineRequests.get(i);
                    if(req.block == null) continue;
                    if(i == lineRequests.size - 1 && req.block.rotate){
                        drawArrow(block, req.x, req.y, req.rotation);
                    }
                    drawRequest(req);
                }
                lineRequests.each(this::drawOverRequest);
            }else if(isPlacing()){
                if(block.rotate){
                    drawArrow(block, cursorX, cursorY, rotation);
                }
                Draw.color();
                boolean valid = validPlace(cursorX, cursorY, block, rotation);
                drawRequest(cursorX, cursorY, block, rotation);
                block.drawPlace(cursorX, cursorY, rotation, valid);

                if(block.saveConfig){
                    Draw.mixcol(!valid ? Pal.breakInvalid : Color.white, (!valid ? 0.4f : 0.24f) + Mathf.absin(Time.globalTime, 6f, 0.28f));
                    brequest.set(cursorX, cursorY, rotation, block);
                    brequest.config = block.lastConfig;
                    block.drawRequestConfig(brequest, allRequests());
                    brequest.config = null;
                    Draw.reset();
                }

            }else if(mode == payloadPlace){
                if(player.unit() instanceof Payloadc){
                    Payload payload = ((Payloadc)player.unit()).hasPayload()? ((Payloadc)player.unit()).payloads().peek() : null;
                    if(payload != null){
                        if(payload instanceof BuildPayload){
                            drawRequest(cursorX, cursorY, ((BuildPayload)payload).block(), 0);
                            if(input.keyTap(Binding.select) && validPlace(cursorX, cursorY, ((BuildPayload)payload).block(), 0)){
                                Navigation.follow(new WaypointPath(new Seq<>(new Waypoint[]{new PositionWaypoint(player.x, player.y), new PayloadDropoffWaypoint(cursorX, cursorY)})));
                                NavigationState previousState = Navigation.state;
                                Navigation.currentlyFollowing.addListener(() -> Navigation.state = previousState);
                                if(Navigation.state == NavigationState.RECORDING){
                                    Navigation.addWaypointRecording(new PayloadDropoffWaypoint(cursorX, cursorY));
                                }
                                mode = none;
                            }
                        }
                    }
                }
            }
        }

        Draw.reset();
    }

    @Override
    public void update(){
        super.update();

        if(Core.input.keyTap(Binding.player_list) && (scene.getKeyboardFocus() == null || scene.getKeyboardFocus().isDescendantOf(ui.listfrag.content) || scene.getKeyboardFocus().isDescendantOf(ui.minimapfrag.elem))){
            ui.listfrag.toggle();
        }

        conveyorPlaceNormal = input.keyDown(Binding.toggle_placement_modifiers);

        // Holding o hides units, pressing shift + o inverts the state; holding o will now show them.
        if ((input.keyTap(Binding.invisible_units) || (input.keyRelease(Binding.invisible_units) && !input.shift())) && scene.getKeyboardFocus() == null) {
            Client.hideUnits = !Client.hideUnits;
        }

        if(Navigation.state == NavigationState.RECORDING){
            if(input.keyTap(Binding.place_waypoint) && scene.getKeyboardFocus() == null){
                Navigation.addWaypointRecording(new PositionWaypoint(player.x, player.y));
            }
        }

        if(input.keyTap(Binding.show_turret_ranges)){
            Client.showingTurrets = !Client.showingTurrets;
        }

        if(input.keyTap(Binding.stop_following_path) && scene.getKeyboardFocus() == null){
            Navigation.stopFollowing();
        }

        if(input.keyTap(Binding.auto_build) && scene.getKeyboardFocus() == null){
            Navigation.follow(new BuildPath());
        }

        if(input.keyTap(Binding.auto_repair) && scene.getKeyboardFocus() == null){
            Navigation.follow(new RepairPath());
        }

        if(input.keyTap(Binding.toggle_strict_mode) && scene.getKeyboardFocus() == null){
            settings.put("assumeunstrict", !settings.getBool("assumeunstrict"));
        }

        boolean panCam = false;
        float camSpeed = (!Core.input.keyDown(Binding.boost) ? panSpeed : panBoostSpeed) * Time.delta;

        if(input.keyTap(Binding.navigate_to_camera) && scene.getKeyboardFocus() == null && selectRequests.isEmpty()){ // Navigates to cursor despite the bind name
            Navigation.navigateTo(input.mouseWorld());
        }

        if(input.keyDown(Binding.pan) && !scene.hasField() && !scene.hasDialog()){
            panCam = true;
            panning = true;
        }

        if(input.keyDown(Binding.freecam_modifier) && (input.axis(Binding.move_x) != 0f || input.axis(Binding.move_y) != 0f) && scene.getKeyboardFocus() == null){
            panning = true;
            Spectate.pos = null;
            float speed = Time.delta;
            speed *= camera.width;
            speed /= 75f;
            camera.position.add(input.axis(Binding.move_x) * speed, input.axis(Binding.move_y) * speed);
        }

        if(input.keyDown(Binding.drop_payload) && scene.getKeyboardFocus() == null){
            mode = payloadPlace;
        }
        if(input.keyRelease(Binding.drop_payload) && scene.getKeyboardFocus() == null){
            mode = none;
        }

        if (input.keyDown(Binding.find_modifier) && input.keyRelease(Binding.find)) {
            Client.mapping.showFindDialog();
        }

//        if((Math.abs(Core.input.axis(Binding.move_x)) > 0 || Math.abs(Core.input.axis(Binding.move_y)) > 0 || input.keyDown(Binding.mouse_move)) && (!scene.hasField())){
//            panning = false;
//        }


        //TODO awful UI state checking code
        if(((player.dead() || state.isPaused()) && !ui.chatfrag.shown()) && !scene.hasField() && !scene.hasDialog()){
            if(input.keyDown(Binding.mouse_move)){
                panCam = true;
            }

            Core.camera.position.add(Tmp.v1.setZero().add(Core.input.axis(Binding.move_x), Core.input.axis(Binding.move_y)).nor().scl(camSpeed));
        }else if(!player.dead() && !panning){
            Core.camera.position.lerpDelta(player, Core.settings.getBool("smoothcamera") ? 0.08f : 1f);
        }

        if(panCam){
            Core.camera.position.x += Mathf.clamp((Core.input.mouseX() - Core.graphics.getWidth() / 2f) * panScale, -1, 1) * camSpeed;
            Core.camera.position.y += Mathf.clamp((Core.input.mouseY() - Core.graphics.getHeight() / 2f) * panScale, -1, 1) * camSpeed;
        }

        shouldShoot = !scene.hasMouse();
        Tile cursor = tileAt(Core.input.mouseX(), Core.input.mouseY());

        if(!scene.hasMouse()){
            if(Core.input.keyDown(Binding.tile_actions_menu_modifier) && Core.input.keyTap(Binding.select) && cursor != null){ // Tile actions / alt click menu
                int itemHeight = 30;
                Table table = new Table(Tex.buttonTrans);
                table.setWidth(400);
                table.margin(10);
                table.fill();
                table.touchable = Touchable.enabled; // This is needed
                table.defaults().height(itemHeight).padTop(5).fillX();
                try {
                    table.add(cursor.block().localizedName + ": (" + cursor.x + ", " + cursor.y + ")").height(itemHeight).left().growX().fillY().padTop(-5);
                } catch (Exception e) {ui.chatfrag.addMessage(e.getMessage(), "client", Color.scarlet);}

                table.row().fill();
                table.button("View log", () -> { // Tile Logs
                    BaseDialog dialog = new BaseDialog("Logs");
                    dialog.cont.add(new ScrollPane(cursor.getLog().toTable())).center();
                    dialog.addCloseButton();

                    dialog.show();
                    table.remove();
                });

                table.row().fill();
                table.button("Unit Picker", () -> {// Unit Picker / Sniper
                    ui.unitPicker.show();
                    table.remove();
                });

                table.row().fill();
                table.button("Teleport to Cursor", () -> {
                    NetClient.setPosition(World.unconv(cursor.x), World.unconv(cursor.y));
                    table.remove();
                });

                table.setHeight(itemHeight * (table.getRows() + 1) + 10 * (table.getRows() + 1));
                table.setPosition(input.mouseX() - 1, input.mouseY() + 1, Align.topLeft); // Offset by 1 pixel so the code below doesn't trigger instantly
                table.update(() -> {
                    if(input.keyTap(Binding.select) && !table.hasMouse()){
                        table.remove();
                    }
                });
                scene.add(table);
            }
            if(Core.input.keyDown(Binding.control) && Core.input.keyTap(Binding.select)){
                Unit on = selectedUnit();
                if(on != null){
                    Call.unitControl(player, on);
                    shouldShoot = false;
                }
            }
        }

        if(!player.dead() && !state.isPaused() && !(Core.scene.getKeyboardFocus() instanceof TextField)){
            if(!Navigation.isFollowing()){
                updateMovement(player.unit());
            }

            if(Core.input.keyDown(Binding.respawn) && !player.unit().spawnedByCore() && !scene.hasField()){
                Call.unitClear(player);
                controlledType = null;
            }
        }

        if(Core.input.keyRelease(Binding.select)){
            player.shooting = false;
        }

        if(state.isGame() && !scene.hasDialog() && !(scene.getKeyboardFocus() instanceof TextField)){
            if(Core.input.keyTap(Binding.minimap)) ui.minimapfrag.toggle();
            if(Core.input.keyTap(Binding.planet_map) && state.isCampaign()) ui.planet.toggle();
            if(Core.input.keyTap(Binding.research) && state.isCampaign()) ui.research.toggle();
        }

        if(state.isMenu() || Core.scene.hasDialog()) return;

        //zoom camera
        if((!Core.scene.hasScroll() || Core.input.keyDown(Binding.diagonal_placement)) && !ui.chatfrag.shown() && Math.abs(Core.input.axisTap(Binding.zoom)) > 0
            && !Core.input.keyDown(Binding.rotateplaced) && (Core.input.keyDown(Binding.diagonal_placement) || ((!player.isBuilder() || !isPlacing() || !block.rotate) && selectRequests.isEmpty()))){
            renderer.scaleCamera(Core.input.axisTap(Binding.zoom));
        }

        if(player.dead()){
            cursorType = SystemCursor.arrow;
            return;
        }

        pollInput();

        //deselect if not placing
        if(!isPlacing() && mode == placing){
            mode = none;
        }

        if(player.shooting && !canShoot()){
            player.shooting = false;
        }

        if(isPlacing() && player.isBuilder()){
            cursorType = SystemCursor.hand;
            selectScale = Mathf.lerpDelta(selectScale, 1f, 0.2f);
        }else{
            selectScale = 0f;
        }

        if(!Core.input.keyDown(Binding.diagonal_placement) && Math.abs((int)Core.input.axisTap(Binding.rotate)) > 0){
            rotation = Mathf.mod(rotation + (int)Core.input.axisTap(Binding.rotate), 4);

            if(sreq != null){
                sreq.rotation = Mathf.mod(sreq.rotation + (int)Core.input.axisTap(Binding.rotate), 4);
            }

            if(isPlacing() && mode == placing){
                updateLine(selectX, selectY);
            }else if(!selectRequests.isEmpty() && !ui.chatfrag.shown()){
                rotateRequests(selectRequests, Mathf.sign(Core.input.axisTap(Binding.rotate)));
            }
        }

        if(cursor != null){
            if(cursor.build != null){
                cursorType = cursor.build.getCursor();
            }

            if((isPlacing() && player.isBuilder()) || !selectRequests.isEmpty()){
                cursorType = SystemCursor.hand;
            }

            if(!isPlacing() && canMine(cursor)){
                cursorType = ui.drillCursor;
            }

            if(getRequest(cursor.x, cursor.y) != null && mode == none){
                cursorType = SystemCursor.hand;
            }

            if(canTapPlayer(Core.input.mouseWorld().x, Core.input.mouseWorld().y)){
                cursorType = ui.unloadCursor;
            }

            if(cursor.build != null && cursor.interactable(player.team()) && !isPlacing() && Math.abs(Core.input.axisTap(Binding.rotate)) > 0 && Core.input.keyDown(Binding.rotateplaced) && cursor.block().rotate && cursor.block().quickRotate){
                Call.rotateBlock(player, cursor.build, Core.input.axisTap(Binding.rotate) > 0);
            }
        }

        if(input.keyTap(Binding.reset_camera) && scene.getKeyboardFocus() == null && (cursor == null || cursor.build == null || !(cursor.build.block.rotate && cursor.build.block.quickRotate && cursor.build.interactable(player.team())))){
            panning = false;
            Spectate.pos = null;
        }

        if(!Core.scene.hasMouse()){
            Core.graphics.cursor(cursorType);
        }

        cursorType = SystemCursor.arrow;
    }

    @Override
    public void useSchematic(Schematic schem){
        block = null;
        schematicX = tileX(getMouseX());
        schematicY = tileY(getMouseY());

        selectRequests.clear();
        selectRequests.addAll(schematics.toRequests(schem, schematicX, schematicY));
        mode = none;
    }

    @Override
    public boolean isBreaking(){
        return mode == breaking;
    }

    @Override
    public void buildPlacementUI(Table table){
        table.image().color(Pal.gray).height(4f).colspan(4).growX();
        table.row();
        table.left().margin(0f).defaults().size(48f).left();

        table.button(Icon.paste, Styles.clearPartiali, () -> {
            ui.schematics.show();
        }).tooltip("@schematics");

        table.button(Icon.book, Styles.clearPartiali, () -> {
            ui.database.show();
        }).tooltip("@database");

        table.button(Icon.tree, Styles.clearPartiali, () -> {
            ui.research.show();
        }).visible(() -> state.isCampaign()).tooltip("@research");

        table.button(Icon.map, Styles.clearPartiali, () -> {
            ui.planet.show();
        }).visible(() -> state.isCampaign()).tooltip("@planetmap");
    }

    void pollInput(){
        if(scene.getKeyboardFocus() instanceof TextField) return;

        Tile selected = tileAt(Core.input.mouseX(), Core.input.mouseY());
        int cursorX = tileX(Core.input.mouseX());
        int cursorY = tileY(Core.input.mouseY());
        int rawCursorX = World.toTile(Core.input.mouseWorld().x), rawCursorY = World.toTile(Core.input.mouseWorld().y);

        //automatically pause building if the current build queue is empty
        if(Core.settings.getBool("buildautopause") && isBuilding && !player.unit().isBuilding()){
            isBuilding = false;
            buildWasAutoPaused = true;
        }

        if(!selectRequests.isEmpty()){
            int shiftX = rawCursorX - schematicX, shiftY = rawCursorY - schematicY;

            selectRequests.each(s -> {
                s.x += shiftX;
                s.y += shiftY;
            });

            schematicX += shiftX;
            schematicY += shiftY;
        }

        if(Core.input.keyTap(Binding.deselect) && !isPlacing()){
            player.unit().mineTile = null;
        }

        if(Core.input.keyTap(Binding.clear_building)){
            player.unit().clearBuilding();
        }

        if(Core.input.keyTap(Binding.schematic_select) && !Core.scene.hasKeyboard() && mode != breaking){
            schemX = rawCursorX;
            schemY = rawCursorY;
        }

        if(Core.input.keyTap(Binding.schematic_menu) && !Core.scene.hasKeyboard()){
            if(ui.schematics.isShown()){
                ui.schematics.hide();
            }else{
                ui.schematics.show();
            }
        }

        if(Core.input.keyTap(Binding.clear_building) || isPlacing()){
            lastSchematic = null;
            selectRequests.clear();
        }

        if(Core.input.keyRelease(Binding.schematic_select) && !Core.scene.hasKeyboard() && selectX == -1 && selectY == -1 && schemX != -1 && schemY != -1){
            lastSchematic = schematics.create(schemX, schemY, rawCursorX, rawCursorY);
            useSchematic(lastSchematic);
            if(selectRequests.isEmpty()){
                lastSchematic = null;
            }
            schemX = -1;
            schemY = -1;
        }

        if(!selectRequests.isEmpty()){
            if(Core.input.keyTap(Binding.schematic_flip_x)){
                flipRequests(selectRequests, true);
            }

            if(Core.input.keyTap(Binding.schematic_flip_y)){
                flipRequests(selectRequests, false);
            }
        }

        if(sreq != null){
            float offset = ((sreq.block.size + 2) % 2) * tilesize / 2f;
            float x = Core.input.mouseWorld().x + offset;
            float y = Core.input.mouseWorld().y + offset;
            sreq.x = (int)(x / tilesize);
            sreq.y = (int)(y / tilesize);
        }

        if(block == null || mode != placing){
            lineRequests.clear();
        }

        if(Core.input.keyTap(Binding.pause_building)){
            isBuilding = !isBuilding;
            buildWasAutoPaused = false;

            if(isBuilding){
                player.shooting = false;
            }
        }

        if((cursorX != lastLineX || cursorY != lastLineY) && isPlacing() && mode == placing){
            updateLine(selectX, selectY);
            lastLineX = cursorX;
            lastLineY = cursorY;
        }

        if(Core.input.keyTap(Binding.select) && !Core.scene.hasMouse()){
            if(selected != null){
                Call.tileTap(player, selected);
            }

            BuildPlan req = getRequest(cursorX, cursorY);

            if(Core.input.keyDown(Binding.break_block)){
                mode = none;
            }else if(!selectRequests.isEmpty()){
                flushRequests(selectRequests);
            }else if(isPlacing()){
                selectX = cursorX;
                selectY = cursorY;
                lastLineX = cursorX;
                lastLineY = cursorY;
                mode = placing;
                updateLine(selectX, selectY);
            }else if(req != null && !req.breaking && mode == none && !req.initialized){
                sreq = req;
            }else if(req != null && req.breaking){
                deleting = true;
            }else if(selected != null){
                boolean mine = false;
                if (settings.getBool("doubleclicktomine")) {
                    if (canMine(selected)) {
                        if (Time.timeSinceMillis(lastMineClicked) < 400) {
                            mine = tryBeginMine(selected);
                        } else {
                            lastMineClicked = Time.millis();
                        }
                    } else {
                        if (player.unit() != null) {
                            player.unit().mineTile = null;
                        }
                    }
                } else {
                    mine = tryBeginMine(selected);
                }
                //only begin shooting if there's no cursor event
                if(!tryTapPlayer(Core.input.mouseWorld().x, Core.input.mouseWorld().y) && !tileTapped(selected.build) && !player.unit().activelyBuilding() && !droppingItem &&
                    !mine && player.unit().mineTile == null && !Core.scene.hasKeyboard()){
                    player.shooting = shouldShoot;
                }
            }else if(!Core.scene.hasKeyboard()){ //if it's out of bounds, shooting is just fine
                player.shooting = shouldShoot;
            }
        }else if(Core.input.keyTap(Binding.deselect) && isPlacing()){
            block = null;
            mode = none;
        }else if(Core.input.keyTap(Binding.deselect) && !selectRequests.isEmpty()){
            selectRequests.clear();
            lastSchematic = null;
        }else if(Core.input.keyTap(Binding.break_block) && !Core.scene.hasMouse() && player.isBuilder()){
            //is recalculated because setting the mode to breaking removes potential multiblock cursor offset
            deleting = false;
            mode = breaking;
            selectX = tileX(Core.input.mouseX());
            selectY = tileY(Core.input.mouseY());
            schemX = rawCursorX;
            schemY = rawCursorY;
        }

        if(Core.input.keyDown(Binding.select) && mode == none && !isPlacing() && deleting){
            BuildPlan req = getRequest(cursorX, cursorY);
            if(req != null && req.breaking){
                player.unit().plans().remove(req);
            }
        }else{
            deleting = false;
        }

        if(mode == placing && block != null){
            if(!overrideLineRotation && !Core.input.keyDown(Binding.diagonal_placement) && (selectX != cursorX || selectY != cursorY) && ((int)Core.input.axisTap(Binding.rotate) != 0)){
                rotation = ((int)((Angles.angle(selectX, selectY, cursorX, cursorY) + 45) / 90f)) % 4;
                overrideLineRotation = true;
            }
        }else{
            overrideLineRotation = false;
        }

        if(Core.input.keyRelease(Binding.break_block) && Core.input.keyDown(Binding.schematic_select) && mode == breaking){
            lastSchematic = schematics.create(schemX, schemY, rawCursorX, rawCursorY);
            schemX = -1;
            schemY = -1;
        }

        if(Core.input.keyRelease(Binding.break_block) || Core.input.keyRelease(Binding.select)){

            if(mode == placing && block != null){ //touch up while placing, place everything in selection
                flushRequests(lineRequests);
                lineRequests.clear();
                Events.fire(new LineConfirmEvent());
            }else if(mode == breaking){ //touch up while breaking, break everything in selection
                removeSelection(selectX, selectY, cursorX, cursorY, /*!Core.input.keyDown(Binding.schematic_select) ? maxLength :*/ Vars.maxSchematicSize);
                if(lastSchematic != null){
                    useSchematic(lastSchematic);
                    lastSchematic = null;
                }
            }
            selectX = -1;
            selectY = -1;

            tryDropItems(selected == null ? null : selected.build, Core.input.mouseWorld().x, Core.input.mouseWorld().y);

            if(sreq != null){
                if(getRequest(sreq.x, sreq.y, sreq.block.size, sreq) != null){
                    player.unit().plans().remove(sreq, true);
                }
                sreq = null;
            }

            mode = none;
        }

        if(Core.input.keyTap(Binding.toggle_block_status)){
            Core.settings.put("blockstatus", !Core.settings.getBool("blockstatus"));
        }

        if(Core.input.keyTap(Binding.toggle_power_lines)){
            if(Core.settings.getInt("lasersopacity") == 0){
                Core.settings.put("lasersopacity", Core.settings.getInt("preferredlaseropacity", 100));
            }else{
                Core.settings.put("preferredlaseropacity", Core.settings.getInt("lasersopacity"));
                Core.settings.put("lasersopacity", 0);
            }
        }
    }

    @Override
    public boolean selectedBlock(){
        return isPlacing() && mode != breaking;
    }

    @Override
    public float getMouseX(){
        return Core.input.mouseX();
    }

    @Override
    public float getMouseY(){
        return Core.input.mouseY();
    }

    @Override
    public void updateState(){
        super.updateState();

        if(state.isMenu()){
            droppingItem = false;
            mode = none;
            block = null;
            sreq = null;
            selectRequests.clear();
        }
    }

    protected void updateMovement(Unit unit){
        boolean omni = unit.type.omniMovement;

        float speed = unit.realSpeed();
        float xa = Core.input.axis(Binding.move_x);
        float ya = Core.input.axis(Binding.move_y);
        if(input.keyDown(Binding.freecam_modifier)){
            xa = ya = 0f;
        }
        boolean boosted = (unit instanceof Mechc && unit.isFlying());

        movement.set(xa, ya).nor().scl(speed);
        if(Core.input.keyDown(Binding.mouse_move)){
            movement.add(input.mouseWorld().sub(player).scl(1f / 25f * speed)).limit(speed);
        }

        float mouseAngle = Angles.mouseAngle(unit.x, unit.y);
        boolean aimCursor = omni && player.shooting && unit.type.hasWeapons() && unit.type.faceTarget && !boosted && unit.type.rotateShooting;

        if(aimCursor){
            unit.lookAt(mouseAngle);
        }else{
            unit.lookAt(unit.prefRotation());
        }

        if(omni){
            unit.moveAt(movement);
        }else{
            unit.moveAt(Tmp.v2.trns(unit.rotation, movement.len()));
            if(!movement.isZero()){
                unit.vel.rotateTo(movement.angle(), unit.type.rotateSpeed * Math.max(Time.delta, 1));
            }
        }

        unit.aim(unit.type.faceTarget ? Core.input.mouseWorld() : Tmp.v1.trns(unit.rotation, Core.input.mouseWorld().dst(unit)).add(unit.x, unit.y));
        unit.controlWeapons(true, player.shooting && !boosted);

        // if autoboost, invert the behavior of the boost key
        player.boosting = (Core.settings.getBool("autoboost") != input.keyDown(Binding.boost)) && !movement.isZero();
        player.mouseX = unit.aimX();
        player.mouseY = unit.aimY();

        //update payload input
        if(unit instanceof Payloadc){
            if(Core.input.keyTap(Binding.pickupCargo)){
                tryPickupPayload();
            }

            if(Core.input.keyTap(Binding.dropCargo)){
                tryDropPayload();
            }
        }

        //update commander unit
        if(Core.input.keyTap(Binding.command) && unit.type.commandLimit > 0){
            Call.unitCommand(player);
        }
    }
}
