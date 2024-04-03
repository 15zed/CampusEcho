// 删除帖子
async function deletePost(id) {
    if (confirm('确定要删除这条帖子吗？')) {
        try {
            const response = await fetch(`/api/delete/${id}`, {
                method: 'DELETE',
            });

            if (response.ok) {
                //这里不调用renderPosts(updatedPosts)函数来渲染帖子，因为这个函数渲染后的页面效果和原来页面使用thymeleaf渲染的效果不一样
                window.location.reload();//重新加载页面
            } else {
                console.error('Failed to delete post.');
            }
        } catch (error) {
            console.error('Error deleting post:', error);
        }
    }
}

// 发表帖子
async function homePost() {
    const text = document.getElementById("post-text").value;
    const imageFiles = document.getElementById("post-image").files;
    const category = document.getElementById("category").value;

    // 获取用户头像路径（假设在登录时已经获取到）
    const jsonUser = document.getElementById("user").innerText;
    const user = JSON.parse(jsonUser);

    const userAvatarUrl = user.head; // 替换成实际的用户头像路径

    const formattedTime = getFormattedTime();

    // 最大允许上传图片的数量
    const maxImageCount = 6;

    // 检查图片数量是否超过限制
    if (imageFiles.length > maxImageCount) {
        alert(`最多只能上传${maxImageCount}张图片`);
        return;
    }

    const info = {
        userId: user.userId, // 替换成实际的用户ID
        avatar: userAvatarUrl,
        text: text,
        // 其他帖子数据...
        time: formattedTime,
        likes: 0,
        category: category
    };

// 如果有上传的图片，将图片上传并获取图片的URL
    if (imageFiles.length > 0) {
        const imageUrls = [];

        for (let i = 0; i < Math.min(imageFiles.length, maxImageCount); i++) {
            const imageUrl = await uploadImageToBackend(imageFiles[i]);
            imageUrls.push(imageUrl);
        }
        info.img = imageUrls;
    }

    // 向后端发送帖子数据
    const newPostData = await sendPostToBackend(info);

    //调用去主页的函数，由thymeleaf重新渲染页面
    redirectToUserPage(user.userId);
}


// 发表评论
async function homeComment(id, commentText) {
    const currentUserHomeId = document.getElementById("currentUserHomeId").innerText;
    const jsonUser = document.getElementById("user").innerText;
    const user = JSON.parse(jsonUser);

    const formattedTime = getFormattedTime(); // 你之前使用的获取时间的函数

    const commentInfo = {
        userId: user.userId,
        pubId: id,
        text: commentText,
        time: formattedTime,
    };

    // 向后端发送评论数据
    await sendCommentToBackend(commentInfo);
    //调用去主页的函数，由thymeleaf重新渲染页面
    redirectToUserPage(currentUserHomeId);
}

//回到上一页
function getBackToPreviousPage() {
    // window.history.go(-1);
    window.location.href = '/api/welcome';
}

function getBack(currentId) {
    window.location.href = `/user/${currentId}`;
    // window.history.go(-1);
}


//关注
async function follow(userId) {
    const response = await fetch(`/api/follow/${userId}`, {
        method: 'PUT',
    });
    const result = await response.text();
    alert(result);
    window.location.reload();
}


//查看关注列表
function myFollows() {
    window.location.href = '/api/follow/getFollows';
}

//查看粉丝列表
function myFans() {
    window.location.href = '/api/fans/getFans';
}

//取消关注
async function unfollow(userId) {
    const response = await fetch(`/api/unfollow/${userId}`, {
        method: 'PUT',
    });
    const result = await response.text();
    alert(result);
    window.location.reload();
}




