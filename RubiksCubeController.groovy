import groovy.transform.Canonical

@Canonical
class RubiksCube {
    String[][][] cube

    RubiksCube() {
        cube = [
            [['W', 'W', 'W'], ['W', 'W', 'W'], ['W', 'W', 'W']], // Up
            [['R', 'R', 'R'], ['R', 'R', 'R'], ['R', 'R', 'R']], // Right
            [['B', 'B', 'B'], ['B', 'B', 'B'], ['B', 'B', 'B']], // Back
            [['O', 'O', 'O'], ['O', 'O', 'O'], ['O', 'O', 'O']], // Left
            [['G', 'G', 'G'], ['G', 'G', 'G'], ['G', 'G', 'G']], // Front
            [['Y', 'Y', 'Y'], ['Y', 'Y', 'Y'], ['Y', 'Y', 'Y']]  // Down
        ]
    }

    void rotateFace(String face, String direction) {
        // 简单实现旋转逻辑，实际应用中可能需要更复杂的逻辑
        switch (face) {
            case 'U':
                rotateLayer(cube[0], direction)
                break
            case 'R':
                rotateLayer(cube[1], direction)
                break
            case 'B':
                rotateLayer(cube[2], direction)
                break
            case 'L':
                rotateLayer(cube[3], direction)
                break
            case 'F':
                rotateLayer(cube[4], direction)
                break
            case 'D':
                rotateLayer(cube[5], direction)
                break
        }
    }

    private void rotateLayer(String[][] layer, String direction) {
        if (direction == 'CW') {
            layer = [
                [layer[2][0], layer[1][0], layer[0][0]],
                [layer[2][1], layer[1][1], layer[0][1]],
                [layer[2][2], layer[1][2], layer[0][2]]
            ]
        } else if (direction == 'CCW') {
            layer = [
                [layer[0][2], layer[1][2], layer[2][2]],
                [layer[0][1], layer[1][1], layer[2][1]],
                [layer[0][0], layer[1][0], layer[2][0]]
            ]
        }
    }

    String[][][] getCubeState() {
        return cube
    }
}

class RubiksCubeController {
    RubiksCube rubiksCube

    RubiksCubeController() {
        rubiksCube = new RubiksCube()
    }

    def initCube() {
        rubiksCube = new RubiksCube()
        return [status: 'success', cube: rubiksCube.getCubeState()]
    }

    def rotateCube(String face, String direction) {
        rubiksCube.rotateFace(face, direction)
        return [status: 'success', cube: rubiksCube.getCubeState()]
    }

    def getCubeState() {
        return [status: 'success', cube: rubiksCube.getCubeState()]
    }
}