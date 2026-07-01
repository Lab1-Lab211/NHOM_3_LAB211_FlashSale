package controller;

import service.SimulatorResult;
import service.SimulatorService;

import java.util.List;

public class SimulatorController {
    private final SimulatorService simulatorService;

    public SimulatorController(SimulatorService simulatorService) {
        this.simulatorService = simulatorService;
    }

    public List<SimulatorResult> runAll(String flashItemId, int threadCount, int quantityPerThread) {
        return simulatorService.runAll(flashItemId, threadCount, quantityPerThread);
    }
}
