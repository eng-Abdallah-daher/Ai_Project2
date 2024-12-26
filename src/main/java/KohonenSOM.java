import javafx.scene.paint.Color;
import java.util.List;
import java.util.Random;
import java.util.function.Consumer;

public class KohonenSOM {
    private final int gridSizeX;
    private final int gridSizeY;
    private final double initialLearningRate;
    private final int initialRadius;
    private final int maxEpochs;
    private double[][][] weightMatrix;
    private int currentIteration;
    private boolean trainingStopped;
    private List<Color> inputColors;

    public KohonenSOM(int gridSizeX, int gridSizeY, double initialLearningRate, int initialRadius, int maxEpochs, List<Color> inputColors) {
        this.gridSizeX = gridSizeX;
        this.gridSizeY = gridSizeY;
        this.initialLearningRate = initialLearningRate;
        this.initialRadius = initialRadius;
        this.maxEpochs = maxEpochs;
        this.inputColors = inputColors;
        this.weightMatrix = new double[gridSizeX][gridSizeY][3];
        this.currentIteration = 0;
        this.trainingStopped = false;
        initializeWeights();
    }

    private void initializeWeights() {
        Random rand = new Random();
        for (int x = 0; x < gridSizeX; x++) {
            for (int y = 0; y < gridSizeY; y++) {
                weightMatrix[x][y][0] = rand.nextDouble();
                weightMatrix[x][y][1] = rand.nextDouble();
                weightMatrix[x][y][2] = rand.nextDouble();
            }
        }
    }

    private int[] findBMU(double[] inputVector) {
        int[] bmuCoords = new int[2];
        double minDistance = Double.MAX_VALUE;
        for (int x = 0; x < gridSizeX; x++) {
            for (int y = 0; y < gridSizeY; y++) {
                double distance = Math.sqrt(
                        Math.pow(inputVector[0] - weightMatrix[x][y][0], 2) +
                                Math.pow(inputVector[1] - weightMatrix[x][y][1], 2) +
                                Math.pow(inputVector[2] - weightMatrix[x][y][2], 2)
                );
                if (distance < minDistance) {
                    minDistance = distance;
                    bmuCoords[0] = x;
                    bmuCoords[1] = y;
                }
            }
        }
        return bmuCoords;
    }

    public void train(int maxIterations, Consumer<Integer> onEpochComplete) {
        Random rand = new Random();
        double learningRate = initialLearningRate;
        int radius = initialRadius;

        for (currentIteration = 0; currentIteration < maxIterations && !trainingStopped; currentIteration++) {
            if (onEpochComplete != null) {
                onEpochComplete.accept(currentIteration);
            }

            Color input = inputColors.get(rand.nextInt(inputColors.size()));
            double[] inputVector = {input.getRed(), input.getGreen(), input.getBlue()};

            int[] bmuCoords = findBMU(inputVector);
            int bmuX = bmuCoords[0];
            int bmuY = bmuCoords[1];

            updateWeights(bmuX, bmuY, inputVector, learningRate, radius);

            learningRate *= 0.995;
            radius = Math.max(radius - 1, 1);

            if (trainingStopped) {
                break;
            }
        }
    }

    private void updateWeights(int bmuX, int bmuY, double[] input, double learningRate, int radius) {
        for (int x = Math.max(0, bmuX - radius); x <= Math.min(gridSizeX - 1, bmuX + radius); x++) {
            for (int y = Math.max(0, bmuY - radius); y <= Math.min(gridSizeY - 1, bmuY + radius); y++) {
                double distanceToBMU = Math.sqrt(Math.pow(x - bmuX, 2) + Math.pow(y - bmuY, 2));
                if (distanceToBMU <= radius) {
                    double influence = Math.exp(-(distanceToBMU * distanceToBMU) / (2 * (radius * radius)));
                    for (int i = 0; i < 3; i++) {
                        weightMatrix[x][y][i] += influence * learningRate * (input[i] - weightMatrix[x][y][i]);
                    }
                }
            }
        }
    }

    public Color getColorAt(int x, int y) {
        if (x < 0 || x >= gridSizeX || y < 0 || y >= gridSizeY) {
            return Color.BLACK;
        }
        return Color.color(weightMatrix[x][y][0], weightMatrix[x][y][1], weightMatrix[x][y][2]);
    }

    public int getCurrentIteration() {
        return currentIteration;
    }

    public void stopTraining() {
        trainingStopped = true;
    }

}
