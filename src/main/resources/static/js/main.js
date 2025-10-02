document.addEventListener('DOMContentLoaded', () => {
    let selectedUserId = null;
    let selectedUserName = '';
    let selectedUserCode = '';

    // リアルタイム時計更新
    function updateTime() {
        const now = new Date();
        document.getElementById('current-time').textContent = now.toLocaleTimeString('ja-JP', { hour: '2-digit', minute: '2-digit' });
        document.getElementById('current-date').textContent = now.toLocaleDateString('ja-JP', { year: 'numeric', month: 'long', day: 'numeric', weekday: 'short' });
    }
    setInterval(updateTime, 1000);
    updateTime();

    // 従業員リストの生成 (APIから取得)
    async function fetchAndRenderEmployees() {
        try {
            const response = await fetch('/api/employees');
            if (!response.ok) {
                throw new Error(`HTTP error! status: ${response.status}`);
            }
            const employees = await response.json();
            
            const listElement = document.getElementById('employee-list');
            listElement.innerHTML = ''; // Clear existing list
            employees.forEach(emp => {
                const item = document.createElement('div');
                item.className = 'employee-item';
                item.textContent = emp.name;
                item.setAttribute('data-id', emp.id);
                item.setAttribute('data-name', emp.name);
                item.setAttribute('data-code', emp.employeeCode);
                item.onclick = () => selectEmployee(emp.id, emp.name, emp.employeeCode, item);
                listElement.appendChild(item);
            });
        } catch (error) {
            console.error('Failed to fetch employees:', error);
            const listElement = document.getElementById('employee-list');
            listElement.innerHTML = '<p style="color: red;">従業員リストの読み込みに失敗しました。</p>';
        }
    }
    
    // 従業員選択時の処理
    window.selectEmployee = function(id, name, code, element) {
        selectedUserId = id;
        selectedUserName = name;
        selectedUserCode = code;
        document.getElementById('selected-name').textContent = name;
        
        // 選択状態のハイライト
        document.querySelectorAll('.employee-item').forEach(item => {
            item.classList.remove('is-selected');
        });
        element.classList.add('is-selected');

        document.getElementById('message-area').textContent = `${name}さんを選択しました。打刻ボタンを押してください。`;
        document.getElementById('message-area').className = 'message-box is-info';

        document.querySelector('.selected-user-info .prompt').style.display = 'none';
    }

    // 打刻処理
    window.stamp = async function(type) {
        const messageArea = document.getElementById('message-area');
        
        if (selectedUserId === null) {
            messageArea.textContent = '⚠️ まずリストから自分の名前を選択してください！';
            messageArea.className = 'message-box is-error';
            return;
        }

        const attendanceData = {
            employeeId: selectedUserId,
            employeeCode: selectedUserCode,
            name: selectedUserName,
            attendanceType: type
        };

        try {
            const response = await fetch('/api/attendance/stamp', {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                },
                body: JSON.stringify(attendanceData),
            });

            if (response.ok) {
                const result = await response.text();
                messageArea.textContent = `✅ ${result}`;
                messageArea.className = 'message-box is-success';
            } else {
                const errorText = await response.text();
                throw new Error(errorText || 'サーバーでエラーが発生しました。');
            }

        } catch (error) {
            console.error('Stamp error:', error);
            messageArea.textContent = `❌ 打刻に失敗しました: ${error.message}`;
            messageArea.className = 'message-box is-error';
        }
        
        // 5秒後に選択状態とメッセージをリセット
        setTimeout(() => {
            selectedUserId = null;
            selectedUserName = '';
            selectedUserCode = '';
            document.getElementById('selected-name').textContent = '';
            document.querySelector('.selected-user-info .prompt').style.display = 'block';
            document.querySelectorAll('.employee-item').forEach(item => {
                item.classList.remove('is-selected');
            });
            messageArea.textContent = '';
            messageArea.className = 'message-box';
        }, 5000);
    }

    fetchAndRenderEmployees();
});;