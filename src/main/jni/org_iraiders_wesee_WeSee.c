/* Hic sunt dracones */

#include <errno.h>
#include <fcntl.h>
#include <stdbool.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <sys/ioctl.h>
#include <sys/stat.h>
#include <sys/types.h>

#include <linux/videodev2.h>

#include "org_iraiders_wesee_WeSee.h"

bool set_control(int camfd, unsigned int id, int value)
{
  struct v4l2_control ctr;
  ctr.id = id;
  ctr.value = value;
  return ioctl(camfd, VIDIOC_S_CTRL, &ctr) != -1;
}

JNIEXPORT void JNICALL Java_org_iraiders_wesee_WeSee_setWhitebalance
  (JNIEnv * env, jobject obj, jint camId)
{
  char buf[20];
  sprintf(buf, "/dev/video%d", camId);

  int camfd = open(buf, O_RDWR | O_NONBLOCK, 0);
  if (camfd == -1)
  {
    fprintf(stderr, "Cannot open %s: %s", buf, strerror(errno));
    return;
  }

  if (!set_control(camfd, V4L2_CID_AUTO_WHITE_BALANCE, 0))
  {
    perror("Could not disable auto white balance");
    return;
  }
  if (!set_control(camfd, V4L2_CID_WHITE_BALANCE_TEMPERATURE, 4000))
  {
    perror("Could not set white balance");
    return;
  }

  if (close(camfd) == -1)
  {
    perror("Could not free camera");
    return;
  }
}
