export interface PdfUploadResponse {
    message: string;
    file_id?: string;
    hash?: string;
}

export async function uploadPdf(file: File, token: string): Promise<PdfUploadResponse> {
    const formData = new FormData();
    formData.append("file", file);

    const response = await fetch(`${process.env.REACT_APP_PDF_UPLOAD_URL}/upload-pdf`, {
        method: "POST",
        headers: {
            Authorization: `Bearer ${token}`,
        },
        body: formData,
    });

    if (!response.ok) {
        const error = await response.text();
        throw new Error(`PDF upload failed: ${error}`);
    }

    return await response.json();
}
