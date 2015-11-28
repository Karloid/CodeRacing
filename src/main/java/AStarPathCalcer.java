import java.util.*;

public class AStarPathCalcer implements PathCalcer {
    public static final int INITIAL_CAPACITY = 300;
    private PolygonsWorld context;
    private double length;

    @Override
    public void setContext(PolygonsWorld context) {
        this.context = context;
    }

    @Override
    public void calcPath() {
        aStarCalc();
    }

    public AStarPathCalcer() {
        path = new ArrayList<Point>();
    }

    private static final int START_NODE = 1;
    private static final int COMMON_NODE = 0;
    private static final double RANDOM_WAY_RATIO = 0f;
    private static final double MOVE_COST = 1;
    private static final boolean BREAK_TIES = false;
    private static final int MAX_LENGTH_PATH = 999;
    private PriorityQueue<Node> openNodes;
    private ArrayList<Node> closedNodes;
    private Point goalPosition;
    private Node startNode;
    private ArrayList<Point> path;

    private void aStarCalc() {
        goalPosition = context.getEndPoint();
        closedNodes = new ArrayList<Node>();
        openNodes = new PriorityQueue<Node>(INITIAL_CAPACITY, new Comparator<Node>() {
            @Override
            public int compare(Node o1, Node o2) {
                if (o1.getF() < o2.getF()) {
                    return -1;
                } else if (o1.getF() == o2.getF()) {
                    return 0;
                } else {
                    return 1;
                }
            }
        });
        startNode = new Node(context.getStartPoint());
        calcF(startNode);
        openNodes.add(startNode);

        Node peek = openNodes.peek();
        Point position = peek.getPosition();
        while (!openNodes.peek().getPosition().equals(goalPosition) && !(peek.getParentsCount() > MAX_LENGTH_PATH)) {
            //  System.out.println("openNodes count: " + openNodes.size());
            Node current = openNodes.peek();
            openNodes.remove(current);
            closedNodes.add(current);
            for (Node neighbor : getNeighbors(current)) {
                double costG = current.getG() + getEuclideDistance(current.getPosition(), neighbor.getPosition());
                if (openNodes.contains(neighbor) && costG < neighbor.getG()) {
                    openNodes.remove(neighbor);
                }
                if (closedNodes.contains(neighbor) && costG < neighbor.getG()) {
                    closedNodes.remove(neighbor);
                }
                if (!openNodes.contains(neighbor) && !closedNodes.contains(neighbor)) {
                    neighbor.setG(costG);
                    neighbor.recalcF();
                    openNodes.add(neighbor);
                    //        sortOpenNodes();
                    neighbor.setParent(current);
                }
            }
        }
        System.out.println();
        System.out.println("======+=+======");
        System.out.println("done A* " + ":" + openNodes.peek().getParentsCount());
        savePath();
    }

    private void savePath() {
        length = 0;
        path = new ArrayList<Point>();
        Node node = openNodes.peek();
        while (true) {
            path.add(node.getPosition());
            if (node.getParent() != null)
                length += Utils.getEuclideDistanceSimple(node.getPosition(), node.getParent().getPosition());
            node = node.getParent();
            if (node == null) {
                break;
            }
        }
        System.out.println(" A* length = " + length);
        Collections.reverse(path);
    }

    private List<Node> getNeighbors(Node node) {
        List<Node> neighbors = new ArrayList<Node>();
        Point position = node.getPosition();
        for (Link link : position.getLinks()) {
            Point pointFromLink = link.getAnotherPoint(position);
            Node newNode = new Node(pointFromLink);
            newNode.setParent(newNode);
            calcF(newNode);
            neighbors.add(newNode);
        }
        return neighbors;
    }


    private Node findNodeByPosition(Point point) {
        for (Node node : openNodes) {
            if (node.getPosition().equals(point)) {
                return node;
            }
        }
        for (Node node : closedNodes) {
            if (node.getPosition().equals(point)) {
                return node;
            }
        }
        return null;
    }

    private void calcF(Node node) {
        //  double heuristik = getManhattanDistance(node.getPosition(), goalPosition);
        double heuristik = getEuclideDistance(node.getPosition(), goalPosition);
        double pathCost = (node.getParent() == null ? 0 : node.getParent().getG()
                + context.getLink(node.getPosition(), node.getParent().getPosition()).getLength());
        double f = heuristik + pathCost;
        node.setF(f);
        node.setG(pathCost);
        node.setH(heuristik);
    }

    private double getEuclideDistance(Point position, Point position1) {
        double distance = Utils.getEuclideDistanceSimple(position, position1);
        if (BREAK_TIES && startNode != null) {
            double dx1 = position.getX() - position1.getX();
            double dy1 = position.getY() - position1.getY();
            double dx2 = startNode.getPosition().getX() - position1.getX();
            double dy2 = startNode.getPosition().getY() - position1.getY();
            double cross = Math.abs(dx1 * dy2 - dx2 * dy1);
            return MOVE_COST * (distance) + cross * 0.001d;
        } else {
            return MOVE_COST * (distance);
        }
    }

    private double getManhattanDistance(Point position, Point position1) {
        double dx = Math.abs(position.getX() - position1.getX());
        double dy = Math.abs(position.getY() - position1.getY());
        if (BREAK_TIES && startNode != null) {
            double dx1 = position.getX() - position1.getX();
            double dy1 = position.getY() - position1.getY();
            double dx2 = startNode.getPosition().getX() - position1.getX();
            double dy2 = startNode.getPosition().getY() - position1.getY();
            double cross = Math.abs(dx1 * dy2 - dx2 * dy1);
            return MOVE_COST * (dx + dy) + cross * 0.001d;
        } else {
            return MOVE_COST * (dx + dy);
        }
    }

    public Point getGoalPosition() {
        return goalPosition;
    }

    public PriorityQueue<Node> getOpenNodes() {
        return openNodes;
    }

    public ArrayList<Node> getClosedNodes() {
        return closedNodes;
    }

    public ArrayList<Point> getPath() {
        return path;
    }

    public class Node {
        private double f;
        private double g;
        private double h;
        private Node parent;

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            Node node = (Node) o;

            if (!position.equals(node.position)) return false;

            return true;
        }

        @Override
        public int hashCode() {
            return position.hashCode();
        }

        public Point getPosition() {
            return position;
        }

        private final Point position;

        public Node(Point position) {
            this.position = position;
        }

        public double getF() {
            return f;
        }

        public void setF(double f) {
            this.f = f;
        }

        public void setG(double g) {
            this.g = g;
        }

        public double getG() {
            return g;
        }

        public void setH(double h) {
            this.h = h;
        }

        public double getH() {
            return h;
        }

        public Node getParent() {
            return parent;
        }

        public void setParent(Node parent) {
            this.parent = parent;
        }

        @Override
        public String toString() {
            return "f: " + getF() + "; g: " + getG() + "; h: " + getH() + "; pos: " + getPosition();
        }

        public void recalcF() {
            setF(getG() + getH());
        }

        public String printParents() {
            if (getParent() != null) {
                return this + " > " + getParent().printParents();
            } else {
                return this.toString();
            }
        }

        public int getParentsCount() {
            int count = 0;
            Node node = this;
            while (true) {
                node = node.getParent();
                if (node == null) {
                    return count;
                }
                count++;
            }
        }
    }
}
