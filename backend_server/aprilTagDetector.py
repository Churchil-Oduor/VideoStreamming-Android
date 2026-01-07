import cv2 as cv
from pupil_apriltags import Detector
import numpy as np


class AprilTagDetector:
    """AprilTag Detection Class"""
    fx, fy = 600, 600
    cx, cy = 320, 240

    camera_params = [fx, fy, cx, cy]

    def __init__(self, tag_size=0.165):
        self.__tag_size = tag_size
        self.__detector = Detector(
                families = "tag36h11",
                nthreads = 1,
                quad_decimate = 1.0,
                quad_sigma = 0.0,
                refine_edges = 1,
                decode_shaperning = 0.25,
                debug = 0
                )


    def pose_detected(self, frame):
        gray = cv.cvtColor(frame, cv.COLOR_BGR2GRAY)
        tags = self.__detector.detect(
                gray,
                estimate_tag_pose=True,
                camera_params=self.camera_params,
                tag_size=self.__tag_size)

        for tag in tags:
            pose_R = tag.pose_R #rotation matrix
            pose_T = tag.pose_t #translation matrix

            T = np.eye(4, dtype=np.float32)
            T[:3, :3] = pose_R
            T[:3, 3] = pose_t.flatten()
            
            return T
