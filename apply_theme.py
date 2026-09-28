import os
import re

base_dir = r"C:/Users/shan/Downloads/kk/src/main/resources/templates"

for root, _, files in os.walk(base_dir):
    for f in files:
        filepath = os.path.join(root, f)
        if f.endswith('.html') and 'doctor\\dashboard.html' not in filepath and 'index.html' not in f and 'admin\\doctor-management.html' not in filepath:
            with open(filepath, 'r', encoding='utf-8') as file:
                content = file.read()
            
            # Simple replace background logic
            content = re.sub(r'background:\s*#f8fafc;', 'background: #020617;', content)
            content = re.sub(r'bg-white\b', 'bg-slate-900 border border-slate-800 text-white', content)
            content = re.sub(r'text-slate-800', 'text-white', content)
            content = re.sub(r'text-slate-500', 'text-slate-300', content)
            content = re.sub(r'bg-slate-50', 'bg-slate-800', content)
            content = re.sub(r'border-slate-200', 'border-slate-700', content)
            
            # Color shifts to emerald/blue theme
            content = re.sub(r'bg-blue-600', 'bg-emerald-600', content)
            content = re.sub(r'text-blue-600', 'text-emerald-500', content)
            content = re.sub(r'bg-indigo-600', 'bg-emerald-600', content)
            content = re.sub(r'text-indigo-600', 'text-emerald-500', content)
            content = re.sub(r'bg-indigo-100', 'bg-emerald-900/40', content)
            content = re.sub(r'text-indigo-700', 'text-emerald-400', content)
            
            # Branding fixes
            content = re.sub(r'MedOS Pro', r'HEALTHCAREPLUS', content)
            content = re.sub(r'HEALTHCARE<span class="font-bold text-blue-600">PLUS</span>', r'HEALTHCARE<span class="font-bold text-emerald-500">PLUS</span>', content)
            
            with open(filepath, 'w', encoding='utf-8') as file:
                file.write(content)

print("Applied dark emerald themes to remaining dashboards.")
