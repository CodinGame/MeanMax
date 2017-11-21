import java.io.*;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

class Referee extends MultiReferee {

    private static final int GAME_VERSION = 3;

    static boolean SPAWN_WRECK = false;
    static int LOOTER_COUNT = 3;
    static boolean REAPER_SKILL_ACTIVE = true;
    static boolean DESTROYER_SKILL_ACTIVE = true;
    static boolean DOOF_SKILL_ACTIVE = true;
    static String EXPECTED = "<x> <y> <power> | SKILL <x> <y> | WAIT";
    

    static {
        switch (GAME_VERSION) {
        case 0:
            SPAWN_WRECK = true;
            LOOTER_COUNT = 1;
            REAPER_SKILL_ACTIVE = false;
            DESTROYER_SKILL_ACTIVE = false;
            DOOF_SKILL_ACTIVE = false;
            EXPECTED = "<x> <y> <power> | WAIT";
            break;
        case 1:
            LOOTER_COUNT = 2;
            REAPER_SKILL_ACTIVE = false;
            DESTROYER_SKILL_ACTIVE = false;
            DOOF_SKILL_ACTIVE = false;
            EXPECTED = "<x> <y> <power> | WAIT";
            break;
        case 2:
            LOOTER_COUNT = 3;
            REAPER_SKILL_ACTIVE = false;
            DOOF_SKILL_ACTIVE = false;
            break;
        default:
        }
    }

    static double MAP_RADIUS = 6000.0;
    static int TANKERS_BY_PLAYER;
    static int TANKERS_BY_PLAYER_MIN = 1;
    static int TANKERS_BY_PLAYER_MAX = 3;

    static double WATERTOWN_RADIUS = 3000.0;

    static int TANKER_THRUST = 500;
    static double TANKER_EMPTY_MASS = 2.5;
    static double TANKER_MASS_BY_WATER = 0.5;
    static double TANKER_FRICTION = 0.40;
    static double TANKER_RADIUS_BASE = 400.0;
    static double TANKER_RADIUS_BY_SIZE = 50.0;
    static int TANKER_EMPTY_WATER = 1;
    static int TANKER_MIN_SIZE = 4;
    static int TANKER_MAX_SIZE = 10;
    static double TANKER_MIN_RADIUS = TANKER_RADIUS_BASE + TANKER_RADIUS_BY_SIZE * TANKER_MIN_SIZE;
    static double TANKER_MAX_RADIUS = TANKER_RADIUS_BASE + TANKER_RADIUS_BY_SIZE * TANKER_MAX_SIZE;
    static double TANKER_SPAWN_RADIUS = 8000.0;
    static int TANKER_START_THRUST = 2000;

    static int MAX_THRUST = 300;
    static int MAX_RAGE = 300;
    static int WIN_SCORE = 50;

    static double REAPER_MASS = 0.5;
    static double REAPER_FRICTION = 0.20;
    static int REAPER_SKILL_DURATION = 3;
    static int REAPER_SKILL_COST = 30;
    static int REAPER_SKILL_ORDER = 0;
    static double REAPER_SKILL_RANGE = 2000.0;
    static double REAPER_SKILL_RADIUS = 1000.0;
    static double REAPER_SKILL_MASS_BONUS = 10.0;

    static double DESTROYER_MASS = 1.5;
    static double DESTROYER_FRICTION = 0.30;
    static int DESTROYER_SKILL_DURATION = 1;
    static int DESTROYER_SKILL_COST = 60;
    static int DESTROYER_SKILL_ORDER = 2;
    static double DESTROYER_SKILL_RANGE = 2000.0;
    static double DESTROYER_SKILL_RADIUS = 1000.0;
    static int DESTROYER_NITRO_GRENADE_POWER = 1000;

    static double DOOF_MASS = 1.0;
    static double DOOF_FRICTION = 0.25;
    static double DOOF_RAGE_COEF = 1.0 / 100.0;
    static int DOOF_SKILL_DURATION = 3;
    static int DOOF_SKILL_COST = 30;
    static int DOOF_SKILL_ORDER = 1;
    static double DOOF_SKILL_RANGE = 2000.0;
    static double DOOF_SKILL_RADIUS = 1000.0;

    static double LOOTER_RADIUS = 400.0;
    static int LOOTER_REAPER = 0;
    static int LOOTER_DESTROYER = 1;
    static int LOOTER_DOOF = 2;

    static int TYPE_TANKER = 3;
    static int TYPE_WRECK = 4;
    static int TYPE_REAPER_SKILL_EFFECT = 5;
    static int TYPE_DOOF_SKILL_EFFECT = 6;
    static int TYPE_DESTROYER_SKILL_EFFECT = 7;

    static double EPSILON = 0.00001;
    static double MIN_IMPULSE = 30.0;
    static double IMPULSE_COEFF = 0.5;

    // Global first free id for all elements on the map 
    static int GLOBAL_ID = 0;

    // Center of the map
    final static Point WATERTOWN = new Point(0, 0);

    // The null collision 
    final static Collision NULL_COLLISION = new Collision(1.0 + EPSILON);

    static class Point {
        double x;
        double y;

        Point(double x, double y) {
            this.x = x;
            this.y = y;
        }

        double distance(Point p) {
            return Math.sqrt((this.x - p.x) * (this.x - p.x) + (this.y - p.y) * (this.y - p.y));
        }

        // Move the point to x and y
        void move(double x, double y) {
            this.x = x;
            this.y = y;
        }

        // Move the point to an other point for a given distance
        void moveTo(Point p, double distance) {
            double d = distance(p);

            if (d < EPSILON) {
                return;
            }

            double dx = p.x - x;
            double dy = p.y - y;
            double coef = distance / d;

            this.x += dx * coef;
            this.y += dy * coef;
        }

        boolean isInRange(Point p, double range) {
            return p != this && distance(p) <= range;
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            long temp;
            temp = Double.doubleToLongBits(x);
            result = prime * result + (int) (temp ^ (temp >>> 32));
            temp = Double.doubleToLongBits(y);
            result = prime * result + (int) (temp ^ (temp >>> 32));
            return result;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) return true;
            if (obj == null) return false;
            if (getClass() != obj.getClass()) return false;
            Point other = (Point) obj;
            if (Double.doubleToLongBits(x) != Double.doubleToLongBits(other.x)) return false;
            if (Double.doubleToLongBits(y) != Double.doubleToLongBits(other.y)) return false;
            return true;
        }
    }

    static class Wreck extends Point {
        int id;
        double radius;
        int water;
        boolean known;
        Player player;

        Wreck(double x, double y, int water, double radius) {
            super(x, y);

            id = GLOBAL_ID++;

            this.radius = radius;
            this.water = water;
        }

        String getFrameId() {
            return id + "@" + water;
        }

        String toFrameData() {
            if (known) {
                return String.valueOf(getFrameId());
            }

            known = true;

            return join(getFrameId(), Math.round(x), Math.round(y), 0, 0, TYPE_WRECK, radius);
        }

        // Reaper harvesting
        public boolean harvest(List<Player> players, Set<SkillEffect> skillEffects) {
            players.forEach(p -> {
                if (isInRange(p.getReaper(), radius) && !p.getReaper().isInDoofSkill(skillEffects)) {
                    p.score += 1;
                    water -= 1;
                }
            });

            return water > 0;
        }
    }

    static abstract class Unit extends Point {
        int type;
        int id;
        double vx;
        double vy;
        double radius;
        double mass;
        double friction;
        boolean known;

        Unit(int type, double x, double y) {
            super(x, y);

            id = GLOBAL_ID++;
            this.type = type;

            vx = 0.0;
            vy = 0.0;

            known = false;
        }

        void move(double t) {
            x += vx * t;
            y += vy * t;
        }

        double speed() {
            return Math.sqrt(vx * vx + vy * vy);
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + id;
            return result;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj)
                return true;
            if (obj == null)
                return false;
            if (getClass() != obj.getClass())
                return false;
            Unit other = (Unit) obj;
            if (id != other.id)
                return false;
            return true;
        }

        String getFrameId() {
            return String.valueOf(id);
        }

        String toFrameData() {
            if (known) {
                return join(getFrameId(), Math.round(x), Math.round(y), Math.round(vx), Math.round(vy));
            }

            known = true;

            return join(getFrameId(), Math.round(x), Math.round(y), Math.round(vx), Math.round(vy), type, Math.round(radius));
        }

        void thrust(Point p, int power) {
            double distance = distance(p);

            // Avoid a division by zero
            if (Math.abs(distance) <= EPSILON) {
                return;
            }

            double coef = (((double) power) / mass) / distance;
            vx += (p.x - this.x) * coef;
            vy += (p.y - this.y) * coef;
        }

        public boolean isInDoofSkill(Set<SkillEffect> skillEffects) {
            return skillEffects.stream().anyMatch(s -> s instanceof DoofSkillEffect && isInRange(s, s.radius + radius));
        }

        void adjust(Set<SkillEffect> skillEffects) {
            x = round(x);
            y = round(y);

            if (isInDoofSkill(skillEffects)) {
                // No friction if we are in a doof skill effect
                vx = round(vx);
                vy = round(vy);
            } else {
                vx = round(vx * (1.0 - friction));
                vy = round(vy * (1.0 - friction));
            }
        }

        // Search the next collision with the map border
        Collision getCollision() {
            // Check instant collision
            if (distance(WATERTOWN) + radius >= MAP_RADIUS) {
                return new Collision(0.0, this);
            }

            // We are not moving, we can't reach the map border
            if (vx == 0.0 && vy == 0.0) {
                return NULL_COLLISION;
            }

            // Search collision with map border
            // Resolving: sqrt((x + t*vx)^2 + (y + t*vy)^2) = MAP_RADIUS - radius <=> t^2*(vx^2 + vy^2) + t*2*(x*vx + y*vy) + x^2 + y^2 - (MAP_RADIUS - radius)^2 = 0
            // at^2 + bt + c = 0;
            // a = vx^2 + vy^2
            // b = 2*(x*vx + y*vy)
            // c = x^2 + y^2 - (MAP_RADIUS - radius)^2

            double a = vx * vx + vy * vy;

            if (a <= 0.0) {
                return NULL_COLLISION;
            }

            double b = 2.0 * (x * vx + y * vy);
            double c = x * x + y * y - (MAP_RADIUS - radius) * (MAP_RADIUS - radius);
            double delta = b * b - 4.0 * a * c;

            if (delta <= 0.0) {
                return NULL_COLLISION;
            }

            double t = (-b + Math.sqrt(delta)) / (2.0 * a);

            if (t <= 0.0) {
                return NULL_COLLISION;
            }

            return new Collision(t, this);
        }

        // Search the next collision with an other unit
        Collision getCollision(Unit u) {
            // Check instant collision
            if (distance(u) <= radius + u.radius) {
                return new Collision(0.0, this, u);
            }

            // Both units are motionless
            if (vx == 0.0 && vy == 0.0 && u.vx == 0.0 && u.vy == 0.0) {
                return NULL_COLLISION;
            }

            // Change referencial
            // Unit u is not at point (0, 0) with a speed vector of (0, 0)
            double x2 = x - u.x;
            double y2 = y - u.y;
            double r2 = radius + u.radius;
            double vx2 = vx - u.vx;
            double vy2 = vy - u.vy;

            // Resolving: sqrt((x + t*vx)^2 + (y + t*vy)^2) = radius <=> t^2*(vx^2 + vy^2) + t*2*(x*vx + y*vy) + x^2 + y^2 - radius^2 = 0
            // at^2 + bt + c = 0;
            // a = vx^2 + vy^2
            // b = 2*(x*vx + y*vy)
            // c = x^2 + y^2 - radius^2 

            double a = vx2 * vx2 + vy2 * vy2;

            if (a <= 0.0) {
                return NULL_COLLISION;
            }

            double b = 2.0 * (x2 * vx2 + y2 * vy2);
            double c = x2 * x2 + y2 * y2 - r2 * r2;
            double delta = b * b - 4.0 * a * c;

            if (delta < 0.0) {
                return NULL_COLLISION;
            }

            double t = (-b - Math.sqrt(delta)) / (2.0 * a);

            if (t <= 0.0) {
                return NULL_COLLISION;
            }

            return new Collision(t, this, u);
        }

        // Bounce between 2 units
        void bounce(Unit u) {
            double mcoeff = (mass + u.mass) / (mass * u.mass);
            double nx = x - u.x;
            double ny = y - u.y;
            double nxnysquare = nx * nx + ny * ny;
            double dvx = vx - u.vx;
            double dvy = vy - u.vy;
            double product = (nx * dvx + ny * dvy) / (nxnysquare * mcoeff);
            double fx = nx * product;
            double fy = ny * product;
            double m1c = 1.0 / mass;
            double m2c = 1.0 / u.mass;

            vx -= fx * m1c;
            vy -= fy * m1c;
            u.vx += fx * m2c;
            u.vy += fy * m2c;

            fx = fx * IMPULSE_COEFF;
            fy = fy * IMPULSE_COEFF;

            // Normalize vector at min or max impulse
            double impulse = Math.sqrt(fx * fx + fy * fy);
            double coeff = 1.0;
            if (impulse > EPSILON && impulse < MIN_IMPULSE) {
                coeff = MIN_IMPULSE / impulse;
            }

            fx = fx * coeff;
            fy = fy * coeff;

            vx -= fx * m1c;
            vy -= fy * m1c;
            u.vx += fx * m2c;
            u.vy += fy * m2c;

            double diff = (distance(u) - radius - u.radius) / 2.0;
            if (diff <= 0.0) {
                // Unit overlapping. Fix positions.
                moveTo(u, diff - EPSILON);
                u.moveTo(this, diff - EPSILON);
            }
        }

        // Bounce with the map border
        void bounce() {
            double mcoeff = 1.0 / mass;
            double nxnysquare = x * x + y * y;
            double product = (x * vx + y * vy) / (nxnysquare * mcoeff);
            double fx = x * product;
            double fy = y * product;

            vx -= fx * mcoeff;
            vy -= fy * mcoeff;

            fx = fx * IMPULSE_COEFF;
            fy = fy * IMPULSE_COEFF;

            // Normalize vector at min or max impulse
            double impulse = Math.sqrt(fx * fx + fy * fy);
            double coeff = 1.0;
            if (impulse > EPSILON && impulse < MIN_IMPULSE) {
                coeff = MIN_IMPULSE / impulse;
            }

            fx = fx * coeff;
            fy = fy * coeff;
            vx -= fx * mcoeff;
            vy -= fy * mcoeff;

            double diff = distance(WATERTOWN) + radius - MAP_RADIUS;
            if (diff >= 0.0) {
                // Unit still outside of the map, reposition it
                moveTo(WATERTOWN, diff + EPSILON);
            }
        }

        public int getExtraInput() {
            return -1;
        }

        public int getExtraInput2() {
            return -1;
        }

        public int getPlayerIndex() {
            return -1;
        }
    }

    static class Tanker extends Unit {
        int water;
        int size;
        Player player;
        boolean killed;

        Tanker(int size, Player player) {
            super(TYPE_TANKER, 0.0, 0.0);

            this.player = player;
            this.size = size;

            water = TANKER_EMPTY_WATER;
            mass = TANKER_EMPTY_MASS + TANKER_MASS_BY_WATER * water;
            friction = TANKER_FRICTION;
            radius = TANKER_RADIUS_BASE + TANKER_RADIUS_BY_SIZE * size;
        }

        String getFrameId() {
            return id + "@" + water;
        }

        Wreck die() {
            // Don't spawn a wreck if our center is outside of the map
            if (distance(WATERTOWN) >= MAP_RADIUS) {
                return null;
            }

            return new Wreck(round(x), round(y), water, radius);
        }

        boolean isFull() {
            return water >= size;
        }

        void play() {
            if (isFull()) {
                // Try to leave the map
                thrust(WATERTOWN, -TANKER_THRUST);
            } else if (distance(WATERTOWN) > WATERTOWN_RADIUS) {
                // Try to reach watertown
                thrust(WATERTOWN, TANKER_THRUST);
            }
        }

        Collision getCollision() {
            // Tankers can go outside of the map
            return NULL_COLLISION;
        }

        public int getExtraInput() {
            return water;
        }

        public int getExtraInput2() {
            return size;
        }
    }

    static abstract class Looter extends Unit {
        int skillCost;
        double skillRange;
        boolean skillActive;

        Player player;

        Point wantedThrustTarget;
        int wantedThrustPower;

        String message;
        Action attempt;
        SkillResult skillResult;

        Looter(int type, Player player, double x, double y) {
            super(type, x, y);

            this.player = player;

            radius = LOOTER_RADIUS;
        }

        SkillEffect skill(Point p) throws TooFarException, NoRageException {
            if (player.rage < skillCost)
                throw new NoRageException();
            if (distance(p) > skillRange)
                throw new TooFarException();

            player.rage -= skillCost;
            return skillImpl(p);
        }

        String toFrameData() {
            if (known) {
                return super.toFrameData();
            }

            return join(super.toFrameData(), player.index);
        }

        public int getPlayerIndex() {
            return player.index;
        }

        abstract SkillEffect skillImpl(Point p);

        public void setWantedThrust(Point target, Integer power) {
            if (power < 0) {
                power = 0;
            }

            wantedThrustTarget = target;
            wantedThrustPower = Math.min(power, MAX_THRUST);
        }

        public void reset() {
            message = null;
            attempt = null;
            skillResult = null;
            wantedThrustTarget = null;
        }
    }

    static class Reaper extends Looter {
        Reaper(Player player, double x, double y) {
            super(LOOTER_REAPER, player, x, y);

            mass = REAPER_MASS;
            friction = REAPER_FRICTION;
            skillCost = REAPER_SKILL_COST;
            skillRange = REAPER_SKILL_RANGE;
            skillActive = REAPER_SKILL_ACTIVE;
        }

        SkillEffect skillImpl(Point p) {
            return new ReaperSkillEffect(TYPE_REAPER_SKILL_EFFECT, p.x, p.y, REAPER_SKILL_RADIUS, REAPER_SKILL_DURATION, REAPER_SKILL_ORDER, this);
        }
    }

    static class Destroyer extends Looter {
        Destroyer(Player player, double x, double y) {
            super(LOOTER_DESTROYER, player, x, y);

            mass = DESTROYER_MASS;
            friction = DESTROYER_FRICTION;
            skillCost = DESTROYER_SKILL_COST;
            skillRange = DESTROYER_SKILL_RANGE;
            skillActive = DESTROYER_SKILL_ACTIVE;
        }

        SkillEffect skillImpl(Point p) {
            return new DestroyerSkillEffect(TYPE_DESTROYER_SKILL_EFFECT, p.x, p.y, DESTROYER_SKILL_RADIUS, DESTROYER_SKILL_DURATION,
                    DESTROYER_SKILL_ORDER, this);
        }
    }

    static class Doof extends Looter {
        Doof(Player player, double x, double y) {
            super(LOOTER_DOOF, player, x, y);

            mass = DOOF_MASS;
            friction = DOOF_FRICTION;
            skillCost = DOOF_SKILL_COST;
            skillRange = DOOF_SKILL_RANGE;
            skillActive = DOOF_SKILL_ACTIVE;
        }

        SkillEffect skillImpl(Point p) {
            return new DoofSkillEffect(TYPE_DOOF_SKILL_EFFECT, p.x, p.y, DOOF_SKILL_RADIUS, DOOF_SKILL_DURATION, DOOF_SKILL_ORDER, this);
        }

        // With flame effects! Yeah!
        int sing() {
            return (int) Math.floor(speed() * DOOF_RAGE_COEF);
        }
    }

    static class Player {
        int score;
        int index;
        int rage;
        Looter[] looters;
        boolean dead;
        Queue<TankerSpawn> tankers;

        Player(int index) {
            this.index = index;

            looters = new Looter[LOOTER_COUNT];
        }

        void kill() {
            dead = true;
        }

        Reaper getReaper() {
            return (Reaper) looters[LOOTER_REAPER];
        }

        Destroyer getDestroyer() {
            return (Destroyer) looters[LOOTER_DESTROYER];
        }

        Doof getDoof() {
            return (Doof) looters[LOOTER_DOOF];
        }
    }

    static class TankerSpawn {
        int size;
        double angle;

        TankerSpawn(int size, double angle) {
            this.size = size;
            this.angle = angle;
        }
    }

    static class Collision {
        double t;
        Unit a;
        Unit b;

        Collision(double t) {
            this(t, null, null);
        }

        Collision(double t, Unit a) {
            this(t, a, null);
        }

        Collision(double t, Unit a, Unit b) {
            this.t = t;
            this.a = a;
            this.b = b;
        }

        Tanker dead() {
            if (a instanceof Destroyer && b instanceof Tanker && b.mass < REAPER_SKILL_MASS_BONUS) {
                return (Tanker) b;
            }

            if (b instanceof Destroyer && a instanceof Tanker && a.mass < REAPER_SKILL_MASS_BONUS) {
                return (Tanker) a;
            }

            return null;
        }
    }

    static abstract class SkillEffect extends Point {
        int id;
        int type;
        double radius;
        int duration;
        int order;
        boolean known;
        Looter looter;

        SkillEffect(int type, double x, double y, double radius, int duration, int order, Looter looter) {
            super(x, y);

            id = GLOBAL_ID++;

            this.type = type;
            this.radius = radius;
            this.duration = duration;
            this.looter = looter;
            this.order = order;
        }

        void apply(List<Unit> units) {
            duration -= 1;
            applyImpl(units.stream().filter(u -> isInRange(u, radius + u.radius)).collect(Collectors.toList()));
        }

        String toFrameData() {
            if (known) {
                return String.valueOf(id);
            }

            known = true;

            return join(id, Math.round(x), Math.round(y), looter.id, 0, type, Math.round(radius));
        }

        abstract void applyImpl(List<Unit> units);

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + id;
            return result;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) return true;
            if (obj == null) return false;
            if (getClass() != obj.getClass()) return false;
            SkillEffect other = (SkillEffect) obj;
            if (id != other.id) return false;
            return true;
        }
    }

    static class ReaperSkillEffect extends SkillEffect {

        ReaperSkillEffect(int type, double x, double y, double radius, int duration, int order, Reaper reaper) {
            super(type, x, y, radius, duration, order, reaper);
        }

        void applyImpl(List<Unit> units) {
            // Increase mass
            units.forEach(u -> u.mass += REAPER_SKILL_MASS_BONUS);
        }
    }

    static class DestroyerSkillEffect extends SkillEffect {

        DestroyerSkillEffect(int type, double x, double y, double radius, int duration, int order, Destroyer destroyer) {
            super(type, x, y, radius, duration, order, destroyer);
        }

        void applyImpl(List<Unit> units) {
            // Push units
            units.forEach(u -> u.thrust(this, -DESTROYER_NITRO_GRENADE_POWER));
        }
    }

    static class DoofSkillEffect extends SkillEffect {

        DoofSkillEffect(int type, double x, double y, double radius, int duration, int order, Doof doof) {
            super(type, x, y, radius, duration, order, doof);
        }

        void applyImpl(List<Unit> units) {
            // Nothing to do now
        }
    }

    static public int round(double x) {
        int s = x < 0 ? -1 : 1;
        return s * (int) Math.round(s * x);
    }

    // Join multiple object into a space separated string
    static public String join(Object... args) {
        return Stream.of(args).map(String::valueOf).collect(Collectors.joining(" "));
    }

    long seed;
    int playerCount;
    List<Unit> units;
    List<Looter> looters;
    List<Tanker> tankers;
    List<Tanker> deadTankers;
    List<Wreck> wrecks;
    List<List<? extends Unit>> unitsByType;
    List<Player> players;
    List<String> frameData;
    Set<SkillEffect> skillEffects;

    void spawnTanker(Player player) {
        TankerSpawn spawn = player.tankers.remove();

        double angle = (player.index + spawn.angle) * Math.PI * 2.0 / ((double) playerCount);

        double cos = Math.cos(angle);
        double sin = Math.sin(angle);

        if (SPAWN_WRECK) {
            // Spawn a wreck directly
            Wreck wreck = new Wreck(cos * WATERTOWN_RADIUS, sin * WATERTOWN_RADIUS, spawn.size,
                    TANKER_RADIUS_BASE + spawn.size * TANKER_RADIUS_BY_SIZE);
            wreck.player = player;

            wrecks.add(wreck);

            return;
        }

        Tanker tanker = new Tanker(spawn.size, player);

        double distance = TANKER_SPAWN_RADIUS + tanker.radius;

        boolean safe = false;
        while (!safe) {
            tanker.move(cos * distance, sin * distance);
            safe = units.stream().allMatch(u -> tanker.distance(u) > tanker.radius + u.radius);
            distance += TANKER_MIN_RADIUS;
        }

        tanker.thrust(WATERTOWN, TANKER_START_THRUST);

        units.add(tanker);
        tankers.add(tanker);
    }

    Looter createLooter(int type, Player player, double x, double y) {
        if (type == LOOTER_REAPER) {
            return new Reaper(player, x, y);
        } else if (type == LOOTER_DESTROYER) {
            return new Destroyer(player, x, y);
        } else if (type == LOOTER_DOOF) {
            return new Doof(player, x, y);
        }

        // Not supposed to happen
        return null;
    }

    void newFrame(double t) {
        frameData.add("#" + String.format(Locale.US, "%.5f", t));
    }

    void addToFrame(Wreck w) {
        frameData.add(w.toFrameData());
    }

    void addToFrame(Unit u) {
        frameData.add(u.toFrameData());
    }

    void addToFrame(SkillEffect s) {
        frameData.add(s.toFrameData());
    }

    void addDeadToFrame(SkillEffect s) {
        frameData.add(join(s.toFrameData(), "d"));
    }

    void addDeadToFrame(Unit u) {
        frameData.add(join(u.toFrameData(), "d"));
    }

    void addDeadToFrame(Wreck w) {
        frameData.add(join(w.toFrameData(), "d"));
    }

    void snapshot() {
        unitsByType.forEach(list -> {
            frameData.addAll(list.stream().map(u -> u.toFrameData()).collect(Collectors.toList()));
        });

        frameData.addAll(wrecks.stream().map(w -> w.toFrameData()).collect(Collectors.toList()));
        frameData.addAll(skillEffects.stream().map(s -> s.toFrameData()).collect(Collectors.toList()));
    }

    @Override
    protected void initReferee(int playerCount, Properties prop) throws InvalidFormatException {
        this.playerCount = playerCount;

        try {
            seed = Long.valueOf(prop.getProperty("seed", String.valueOf(new Random().nextLong())));
        } catch (NumberFormatException e) {
            seed = new Random().nextLong();
        }

        Random random = new Random(seed);

        TANKERS_BY_PLAYER = TANKERS_BY_PLAYER_MIN + random.nextInt(TANKERS_BY_PLAYER_MAX - TANKERS_BY_PLAYER_MIN + 1);

        units = new ArrayList<>();
        looters = new ArrayList<>();
        tankers = new ArrayList<>();
        deadTankers = new ArrayList<>();
        wrecks = new ArrayList<>();
        players = new ArrayList<>();

        unitsByType = new ArrayList<>();
        unitsByType.add(looters);
        unitsByType.add(tankers);

        frameData = new ArrayList<>();

        skillEffects = new TreeSet<>((a, b) -> {
            int order = a.order - b.order;
            
            if (order != 0) {
                return order;
            }
            
            return a.id - b.id; 
        });

        // Create players
        for (int i = 0; i < playerCount; ++i) {
            Player player = new Player(i);
            players.add(player);
        }

        // Generate the map
        Queue<TankerSpawn> queue = new LinkedList<>();
        for (int i = 0; i < 500; ++i) {
            queue.add(new TankerSpawn(TANKER_MIN_SIZE + random.nextInt(TANKER_MAX_SIZE - TANKER_MIN_SIZE),
                    random.nextDouble()));
        }
        players.forEach(p -> p.tankers = new LinkedList<>(queue));

        // Create looters
        for (Player player : players) {
            for (int i = 0; i < LOOTER_COUNT; ++i) {
                Looter looter = createLooter(i, player, 0, 0);
                player.looters[i] = looter;
                units.add(looter);
                looters.add(looter);
            }
        }

        // Random spawns for looters
        boolean finished = false;
        while (!finished) {
            finished = true;

            for (int i = 0; i < LOOTER_COUNT && finished; ++i) {
                double distance = random.nextDouble() * (MAP_RADIUS - LOOTER_RADIUS);
                double angle = random.nextDouble();

                for (Player player : players) {
                    double looterAngle = (player.index + angle) * (Math.PI * 2.0 / ((double) playerCount));
                    double cos = Math.cos(looterAngle);
                    double sin = Math.sin(looterAngle);

                    Looter looter = player.looters[i];
                    looter.move(cos * distance, sin * distance);

                    // If the looter touch a looter, reset everyone and try again
                    if (units.stream().anyMatch(u -> u != looter && looter.distance(u) <= looter.radius + u.radius)) {
                        finished = false;
                        looters.forEach(l -> l.move(0.0, 0.0));
                        break;
                    }
                }
            }
        }

        // Spawn start tankers
        for (int j = 0; j < TANKERS_BY_PLAYER; ++j) {
            for (Player player : players) {
                spawnTanker(player);
            }
        }

        adjust();
        newFrame(1.0);
        snapshot();
    }

    @Override
    protected Properties getConfiguration() {
        Properties properties = new Properties();

        properties.put("seed", seed);

        return properties;
    }

    @Override
    protected String[] getInitInputForPlayer(int playerIdx) {
        List<String> lines = new ArrayList<>();

        // No init input

        return lines.toArray(new String[lines.size()]);
    }

    @Override
    protected void prepare(int round) {
        frameData.clear();
        looters.forEach(Looter::reset);
    }

    int getPlayerId(int id, int forId) {
        // This method can be called with id=-1 because of the default player for units
        if (id < 0) {
            return id;
        }

        if (id == forId) {
            return 0;
        }

        if (id < forId) {
            return id + 1;
        }

        return id;
    }

    @Override
    protected String[] getInputForPlayer(int round, int playerIdx) {
        List<Object> lines = new ArrayList<>();

        // Scores
        // My score is always first
        lines.add(players.get(playerIdx).score);
        for (int i = 0; i < playerCount; ++i) {
            if (i != playerIdx) {
                lines.add(players.get(i).score);
            }
        }

        // Rages
        // My rage is always first
        lines.add(players.get(playerIdx).rage);
        for (int i = 0; i < playerCount; ++i) {
            if (i != playerIdx) {
                lines.add(players.get(i).rage);
            }
        }

        // Units
        List<String> unitsLines = new ArrayList<>();
        // Looters and tankers
        unitsLines.addAll(units.stream().map(
                u -> join(u.id, u.type, getPlayerId(u.getPlayerIndex(), playerIdx), u.mass, round(u.radius), round(u.x), round(u.y), round(u.vx),
                        round(u.vy),
                        u.getExtraInput(), u.getExtraInput2()))
                .collect(Collectors.toList()));
        // Wrecks
        unitsLines.addAll(wrecks.stream().map(w -> join(w.id, TYPE_WRECK, -1, -1, round(w.radius), round(w.x), round(w.y), 0, 0, w.water, -1))
                .collect(Collectors.toList()));
        // Skill effects
        unitsLines.addAll(skillEffects.stream().map(s -> join(s.id, s.type, -1, -1, round(s.radius), round(s.x), round(s.y), 0, 0, s.duration, -1))
                .collect(Collectors.toList()));

        lines.add(unitsLines.size());
        lines.addAll(unitsLines);

        return lines.stream().map(Object::toString).collect(Collectors.toList()).toArray(new String[lines.size()]);
    }

    @Override
    protected int getExpectedOutputLineCountForPlayer(int playerIdx) {
        return 3;
    }

    private static final Pattern PLAYER_MOVE_PATTERN = Pattern
            .compile("(?<x>-?[0-9]{1,9})\\s+(?<y>-?[0-9]{1,9})\\s+(?<power>([0-9]{1,9}))?(?:\\s+(?<message>.+))?");
    private static final Pattern PLAYER_SKILL_PATTERN = Pattern.compile("SKILL\\s+(?<x>-?[0-9]{1,9})\\s+(?<y>-?[0-9]{1,9})(?:\\s+(?<message>.+))?",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern PLAYER_WAIT_PATTERN = Pattern.compile("WAIT(?:\\s+(?<message>.+))?", Pattern.CASE_INSENSITIVE);

    @SuppressWarnings("serial")
    static class NoRageException extends Exception {
    }

    @SuppressWarnings("serial")
    static class TooFarException extends Exception {
    }

    enum Action {
        SKILL, MOVE, WAIT;
    }

    class SkillResult {
        static final int OK = 0;
        static final int NO_RAGE = 1;
        static final int TOO_FAR = 2;
        Point target;
        int code;

        SkillResult(int x, int y) {
            target = new Point(x, y);
            code = OK;
        }

        int getX() {
            return (int) target.x;
        }

        int getY() {
            return (int) target.y;
        }
    }

    @Override
    protected void handlePlayerOutput(int frame, int round, int playerIdx, String[] outputs)
            throws WinException, LostException, InvalidInputException {

        Player player = players.get(playerIdx);
        String expected = EXPECTED;

        for (int i = 0; i < LOOTER_COUNT; ++i) {
            String line = outputs[i];
            Matcher match;
            try {
                Looter looter = players.get(playerIdx).looters[i];

                match = PLAYER_WAIT_PATTERN.matcher(line);
                if (match.matches()) {
                    looter.attempt = Action.WAIT;
                    matchMessage(looter, match);
                    continue;
                }

                match = PLAYER_MOVE_PATTERN.matcher(line);
                if (match.matches()) {
                    looter.attempt = Action.MOVE;
                    int x = Integer.valueOf(match.group("x"));
                    int y = Integer.valueOf(match.group("y"));
                    int power = Integer.valueOf(match.group("power"));

                    looter.setWantedThrust(new Point(x, y), power);
                    matchMessage(looter, match);
                    continue;
                }

                match = PLAYER_SKILL_PATTERN.matcher(line);
                if (match.matches()) {
                    if (!looter.skillActive) {
                        // Don't kill the player for that. Just do a WAIT instead
                        looter.attempt = Action.WAIT;
                        matchMessage(looter, match);
                        continue;
                    }

                    looter.attempt = Action.SKILL;
                    int x = Integer.valueOf(match.group("x"));
                    int y = Integer.valueOf(match.group("y"));

                    SkillResult result = new SkillResult(x, y);
                    looter.skillResult = result;
                    
                    try {
                        SkillEffect effect = looter.skill(new Point(x, y));
                        skillEffects.add(effect);
                    } catch (NoRageException e) {
                        result.code = SkillResult.NO_RAGE;
                    } catch (TooFarException e) {
                        result.code = SkillResult.TOO_FAR;
                    }
                    matchMessage(looter, match);
                    continue;
                }

                throw new InvalidInputException(expected, line);
            } catch (InvalidInputException e) {
                player.kill();
                throw e;
            } catch (Exception e) {
                StringWriter errors = new StringWriter();
                e.printStackTrace(new PrintWriter(errors));
                printError(e.getMessage() + "\n" + errors.toString());
                player.kill();
                throw new InvalidInputException(expected, line);
            }
        }
    }

    @Override
    protected int getMillisTimeForRound() {
        return 50;
    }
    
    private void matchMessage(Looter looter, Matcher match) {
        looter.message = match.group("message");
        if (looter.message != null && looter.message.length() > 19) {
            looter.message = looter.message.substring(0, 17) + "...";
        }
    }

    // Get the next collision for the current round
    // All units are tested
    Collision getNextCollision() {
        Collision result = NULL_COLLISION;

        for (int i = 0; i < units.size(); ++i) {
            Unit unit = units.get(i);

            // Test collision with map border first
            Collision collision = unit.getCollision();

            if (collision.t < result.t) {
                result = collision;
            }

            for (int j = i + 1; j < units.size(); ++j) {
                collision = unit.getCollision(units.get(j));

                if (collision.t < result.t) {
                    result = collision;
                }
            }
        }

        return result;
    }

    // Play a collision
    void playCollision(Collision collision) {
        if (collision.b == null) {
            // Bounce with border
            addToFrame(collision.a);
            collision.a.bounce();
        } else {
            Tanker dead = collision.dead();

            if (dead != null) {
                // A destroyer kill a tanker
                addDeadToFrame(dead);
                deadTankers.add(dead);
                tankers.remove(dead);
                units.remove(dead);

                Wreck wreck = dead.die();

                // If a tanker is too far away, there's no wreck
                if (wreck != null) {
                    wrecks.add(wreck);
                    addToFrame(wreck);
                }
            } else {
                // Bounce between two units
                addToFrame(collision.a);
                addToFrame(collision.b);
                collision.a.bounce(collision.b);
            }
        }
    }

    @Override
    protected void updateGame(int round) throws GameOverException {
        // Apply skill effects
        for (SkillEffect effect : skillEffects) {
            effect.apply(units);
        }

        // Apply thrust for tankers
        for (Tanker t : tankers) {
            t.play();
        }

        // Apply wanted thrust for looters
        for (Player player : players) {
            for (Looter looter : player.looters) {
                if (looter.wantedThrustTarget != null) {
                    looter.thrust(looter.wantedThrustTarget, looter.wantedThrustPower);
                }
            }
        }

        double t = 0.0;

        // Play the round. Stop at each collisions and play it. Reapeat until t > 1.0

        Collision collision = getNextCollision();

        while (collision.t + t <= 1.0) {
            double delta = collision.t;
            units.forEach(u -> u.move(delta));
            t += collision.t;

            newFrame(t);

            playCollision(collision);

            collision = getNextCollision();
        }

        // No more collision. Move units until the end of the round
        double delta = 1.0 - t;
        units.forEach(u -> u.move(delta));

        List<Tanker> tankersToRemove = new ArrayList<>();

        tankers.forEach(tanker -> {
            double distance = tanker.distance(WATERTOWN);
            boolean full = tanker.isFull();

            if (distance <= WATERTOWN_RADIUS && !full) {
                // A non full tanker in watertown collect some water
                tanker.water += 1;
                tanker.mass += TANKER_MASS_BY_WATER;
            } else if (distance >= TANKER_SPAWN_RADIUS + tanker.radius && full) {
                // Remove too far away and not full tankers from the game
                tankersToRemove.add(tanker);
            }
        });

        newFrame(1.0);
        snapshot();

        if (!tankersToRemove.isEmpty()) {
            tankersToRemove.forEach(tanker -> addDeadToFrame(tanker));
        }

        units.removeAll(tankersToRemove);
        tankers.removeAll(tankersToRemove);
        deadTankers.addAll(tankersToRemove);

        // Spawn new tankers for each dead tanker during the round
        deadTankers.forEach(tanker -> spawnTanker(tanker.player));
        deadTankers.clear();

        Set<Wreck> deadWrecks = new HashSet<>();

        // Water collection for reapers
        wrecks = wrecks.stream().filter(w -> {
            boolean alive = w.harvest(players, skillEffects);

            if (!alive) {
                addDeadToFrame(w);
                deadWrecks.add(w);
            }

            return alive;
        }).collect(Collectors.toList());

        if (SPAWN_WRECK) {
            deadWrecks.forEach(w -> spawnTanker(w.player));
        }

        // Round values and apply friction
        adjust();

        // Generate rage
        if (LOOTER_COUNT >= 3) {
            players.forEach(p -> p.rage = Math.min(MAX_RAGE, p.rage + p.getDoof().sing()));
        }

        // Restore masses
        units.forEach(u -> {
            while (u.mass >= REAPER_SKILL_MASS_BONUS) {
                u.mass -= REAPER_SKILL_MASS_BONUS;
            }
        });

        // Remove dead skill effects
        Set<SkillEffect> effectsToRemove = new HashSet<>();
        for (SkillEffect effect : skillEffects) {
            if (effect.duration <= 0) {
                addDeadToFrame(effect);
                effectsToRemove.add(effect);
            }
        }
        skillEffects.removeAll(effectsToRemove);
    }

    protected void adjust() {
        units.forEach(u -> u.adjust(skillEffects));
    }

    @Override
    protected void populateMessages(Properties p) {
        //TODO: write some text
        p.put("Move", "$%d moved looter %d towards (%d,%d) with power %d");
        p.put("SkillFailedTooFar", "$%d %d %d %d");
        p.put("SkillFailedNoRage", "$%d %d %d %d");
        p.put("SkillDestroyer", "$%d %d %d %d");
        p.put("SkillRepear", "$%d %d %d %d");
        p.put("SkillDoof", "$%d %d %d %d");
    }

    @Override
    protected boolean isGameOver() {
        if (players.stream().anyMatch(p -> p.score >= WIN_SCORE)) {
            // We got a winner !
            return true;
        }

        List<Player> alive = players.stream().filter(p -> !p.dead).collect(Collectors.toList());

        if (alive.size() == 1) {
            Player survivor = alive.get(0);

            // If only one player is alive and he got the highest score, end the game now.
            return players.stream().filter(p -> p != survivor).allMatch(p -> p.score < survivor.score);
        }

        // Everyone is dead. End of the game.
        return alive.isEmpty();
    }

    @Override
    protected String[] getInitDataForView() {
        List<String> lines = new ArrayList<>();

        lines.add(String.valueOf(playerCount));
        lines.add(String.valueOf(Math.round(MAP_RADIUS)));
        lines.add(String.valueOf(Math.round(WATERTOWN_RADIUS)));
        lines.add(String.valueOf(Math.round(LOOTER_COUNT)));

        return lines.toArray(new String[lines.size()]);
    }

    @Override
    protected String[] getFrameDataForView(int round, int frame, boolean keyFrame) {
        List<String> lines = new ArrayList<>();

        lines.addAll(players.stream().map(p -> String.valueOf(p.score)).collect(Collectors.toList()));
        lines.addAll(players.stream().map(p -> String.valueOf(p.rage)).collect(Collectors.toList()));
        lines.addAll(looters.stream().map(l -> String.valueOf(l.message == null ? "" : l.message)).collect(Collectors.toList()));
        lines.addAll(frameData);

        return lines.toArray(new String[lines.size()]);
    }

    @Override
    protected String getGameName() {
        return "meanmax";
    }

    @Override
    protected String getHeadlineAtGameStartForConsole() {
        return "";
    }

    @Override
    protected int getMinimumPlayerCount() {
        return 3;
    }

    @Override
    protected boolean showTooltips() {
        return true;
    }

    @Override
    protected String[] getPlayerActions(int playerIdx, int round) {
        return new String[0];
    }

    @Override
    protected boolean isPlayerDead(int playerIdx) {
        return players.get(playerIdx).dead;
    }

    @Override
    protected String getDeathReason(int playerIdx) {
        return "$" + playerIdx + ": Eliminated!";
    }

    @Override
    protected int getScore(int playerIdx) {
        return players.get(playerIdx).score;
    }

    @Override
    protected String[] getGameSummary(int round) {
        List<String> lines = new ArrayList<>();
        /*
        for (Looter looter : looters) {
            if (looter.attempt == Action.MOVE && looter.wantedThrustPower > 0) {
                lines.add(translate("Move", looter.player.index, looter.id, Math.round(looter.wantedThrustTarget.x),
                        Math.round(looter.wantedThrustTarget.y),
                        looter.wantedThrustPower));
            } else if (looter.attempt == Action.SKILL) {
                if (looter.skillResult.code == SkillResult.TOO_FAR) {
                    lines.add(translate("SkillFailedTooFar", looter.player.index, looter.id, looter.skillResult.getX(), looter.skillResult.getY()));
                } else if (looter.skillResult.code == SkillResult.NO_RAGE) {
                    lines.add(translate("SkillFailedNoRage", looter.player.index, looter.id, looter.skillCost, looter.player.rage));
                } else {
                    if (looter.type == LOOTER_DESTROYER) {
                        lines.add(translate("SkillDestroyer", looter.player.index, looter.id, looter.skillResult.getX(), looter.skillResult.getY()));
                    } else if (looter.type == LOOTER_REAPER) {
                        lines.add(translate("SkillRepear", looter.player.index, looter.id, looter.skillResult.getX(), looter.skillResult.getY()));
                    } else if (looter.type == LOOTER_DOOF) {
                        lines.add(translate("SkillDoof", looter.player.index, looter.id, looter.skillResult.getX(), looter.skillResult.getY()));
                    }
                }
            }
        }
        */
        return lines.toArray(new String[lines.size()]);
    }

    @Override
    protected void setPlayerTimeout(int frame, int round, int playerIdx) {
        players.get(playerIdx).kill();
    }

    @Override
    protected int getMaxRoundCount(int playerCount) {
        return 200;
    }

    protected boolean isTurnBasedGame() {
        return false;
    }

    public Referee(InputStream is, PrintStream out, PrintStream err) throws IOException {
        super(is, out, err);
    }

    public static void main(String... args) throws IOException {
        new Referee(System.in, System.out, System.err);
    }
}
