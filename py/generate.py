import os
import torch
import argparse
from diffusers import StableDiffusionPipeline


def args():
    parser = argparse.ArgumentParser()
    parser.add_argument("--name", type=str, help="图片名称，可以补充路径", required=True)
    parser.add_argument("--module",
                        type=str,
                        default="CompVis/stable-diffusion-v1-4",
                        help="大模型类型， CompVis/stable-diffusion-v1-4，stabilityai/stable-diffusion-2-1")
    parser.add_argument(
        "--depth",
        type=int,
        default=50,
        help="number of ddim sampling steps, default: 50, value range: from 10 to 50",
    )
    parser.add_argument(
        "--scale",
        type=int,
        default=15,
        help="number of guidance scale, default: 15, value range: from 7 to 15",
    )
    parser.add_argument(
        "--height",
        type=int,
        default=512,
        help="image height, in pixel space",
    )
    parser.add_argument(
        "--width",
        type=int,
        default=512,
        help="image width, in pixel space",
    )
    parser.add_argument(
        "--prompt",
        type=str,
        nargs="?",
        default="a painting of a virus monster playing guitar",
        help="the prompt to render"
    )
    return parser.parse_args()

device = "cuda" if torch.cuda.is_available() else "cpu"
param = args()
image_name = param.name
if image_name is None:
    print("图片名称未指定, 使用默认名称：img_name")
    image_name = "generated_image_2.png"
print(f"==>image_name: {image_name}")

depth = param.depth
print(f"==>depth: {depth}")

scale = param.scale
print(f"==>scale: {scale}")

height = param.height
width = param.width
print(f"==>width: {height} - {width}")

prompt = param.prompt
if prompt is None:
    prompt = 'A fantasy landscape with mountains and a river'
print(f"==>prompt: {prompt}")

modul = param.module
if modul is None:
    modul = "stabilityai/stable-diffusion-2-1"
print(f"==>module: {modul}")

pipe = StableDiffusionPipeline.from_pretrained(modul, use_auth_token=True).to(device)

generator = torch.manual_seed(42)
with torch.no_grad():
    image = pipe(prompt,
                 num_inference_steps=depth,
                 guidance_scale=scale,
                 height=height,
                 width=width).images[0]

image.save(image_name)
print(f"==>图片已保存为 {os.path.abspath(image_name)}")

