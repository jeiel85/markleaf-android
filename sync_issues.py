import subprocess
import re
import os

def run_command(command):
    try:
        result = subprocess.run(command, capture_output=True, text=True, check=True, shell=True)
        return result.stdout.strip()
    except subprocess.CalledProcessError as e:
        print(f"Error executing command: {e}")
        print(f"Stderr: {e.stderr}")
        return None

def parse_md_to_issues(file_path):
    with open(file_path, 'r', encoding='utf-8') as f:
        content = f.read()

    issues = []
    
    # Extract Group A (Existing Issues)
    group_a = re.findall(r'#### (#\d+ .*?)\n(.*?)(?=\n#### |$)', content, re.DOTALL)
    for title, body in group_a:
        issues.append({
            'title': title.strip(),
            'body': body.strip()
        })

    # Extract Group B (New Issues)
    group_b = re.findall(r'(\d+\. \[Feature\].*?)\n(.*?)(?=\n\d+\. |$)', content, re.DOTALL)
    if not group_b:
         # Try alternative pattern if numbering is different
         group_b = re.findall(r'(\d+\. \[.*?\] .*?)\n(.*?)(?=\n\d+\. |$)', content, re.DOTALL)
         
    for title, body in group_b:
        issues.append({
            'title': title.strip(),
            'body': body.strip()
        })

    return issues

def sync():
    md_file = "docs/ISSUE_BACKLOG_DETAIL.md"
    if not os.path.exists(md_file):
        print(f"File not found: {md_file}")
        return

    issues = parse_md_to_issues(md_file)
    print(f"Found {len(issues)} issues to register.")

    for i, issue in enumerate(issues):
        title = issue['title']
        body = issue['body']
        
        # Clean up title (remove #ID for new issues or keep it for existing)
        # gh issue create --title "title" --body "body"
        print(f"[{i+1}/{len(issues)}] Creating: {title}")
        
        # We use a temp file for body to avoid shell escaping issues with long text
        with open("temp_body.txt", "w", encoding="utf-8") as f:
            f.write(body)
            
        cmd = f'gh issue create --title "{title}" --body-file temp_body.txt'
        run_command(cmd)
        
    if os.path.exists("temp_body.txt"):
        os.remove("temp_body.txt")

if __name__ == "__main__":
    sync()
