import os
import argparse
from jinja2 import Environment, FileSystemLoader

# A simple python script which uses the Jinja2 template engine to render large docker-compose files
# first, install Jinja2 using pip:
#     `pip install jinja2`
# Then, use it to render a docker-compose file like so:
#     `python render_template.py docker-compose.j2 -o custom-docker-compose.yaml`

# Set up command-line argument parsing
parser = argparse.ArgumentParser(description="Render a Jinja2 template for Docker Compose.")
parser.add_argument('input', help='Input Jinja2 template file')
parser.add_argument('-o', '--output', default='docker-compose.yaml', help='Output file name (default: docker-compose.yaml)')
args = parser.parse_args()

# Set the template directory and load the environment
template_dir = os.path.dirname(os.path.abspath(args.input))
env = Environment(loader=FileSystemLoader(template_dir))

# Load the template
template = env.get_template(os.path.basename(args.input))

# Render the template and write the output to the specified file
with open(args.output, 'w') as output_file:
    output_file.write(template.render())
